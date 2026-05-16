package physics;

import java.util.List;
import org.lwjgl.util.vector.Vector3f;
import scene.Entity;
import scene.Scene;

/**
 * Sahnedeki tüm fiziksel nesnelerin etkileşimini, yerçekimini ve çarpışmalarını yöneten ana Fizik Motoru.
 * Kullanıcı tarafından global uzay / dünya şartları (Yerçekimi yönü, sürtünme) buradan değiştirilebilir.
 */
public class PhysicsEngine {

	// Ortamın (Dünyanın) yerçekimi kuvveti. 
	// Dünya için y ekseninde aşağıya doğrudur örn: (0, -9.81f, 0)
	// Uzay ortamı için (0, 0, 0) yapılabilir.
	private Vector3f globalGravity = new Vector3f(0, -9.81f, 0);
	
	// Ortamdaki sürtünme katsayısı (Havanın objeleri ne kadar yavaşlattığı).
	// 0.0 hiç sürtünme yok (Uzay boşluğu), 1.0 anında durdurur.
	private float airDrag = 0.01f;

	public PhysicsEngine() {
		
	}

	/**
	 * Her oyun karesinde (frame) çağrılarak sahnedeki tüm fiziksel objeleri günceller.
	 * 
	 * @param scene Objelerin çekileceği sahne
	 * @param delta Geçen zaman (Delta Time)
	 */
	public void update(Scene scene, float delta) {
		List<Entity> entities = scene.getAllEntities();
		
		// 1. Aşama: Tüm dinamik objelere kuvvetleri (Yerçekimi, hız) uygula
		for (Entity entity : entities) {
			PhysicsComponent physics = entity.getPhysicsComponent();
			
			// Objenin fiziği yoksa veya sabitse (duvar/zemin) hareket ettirme
			if (physics == null || physics.isStatic()) {
				continue;
			}
			
			Vector3f velocity = physics.getVelocity();
			Vector3f acceleration = physics.getAcceleration();
			Vector3f position = entity.getPosition();
			
			// Yerçekimi vektörünü objenin yerçekimi çarpanıyla hesapla
			Vector3f gravityForce = new Vector3f(
				globalGravity.x * physics.getGravityScale(),
				globalGravity.y * physics.getGravityScale(),
				globalGravity.z * physics.getGravityScale()
			);
			
			// Hıza ivmeyi ve yerçekimini ekle (v = v0 + a*t)
			velocity.x += (acceleration.x + gravityForce.x) * delta;
			velocity.y += (acceleration.y + gravityForce.y) * delta;
			velocity.z += (acceleration.z + gravityForce.z) * delta;
			
			// Hava sürtünmesi (Drag) - Hızı yavaş yavaş keser
			velocity.x -= velocity.x * airDrag * delta;
			velocity.y -= velocity.y * airDrag * delta;
			velocity.z -= velocity.z * airDrag * delta;
			
			// Eğer 2D bir obje ise Z eksenindeki hareketleri iptal et
			if (physics.is2D()) {
				velocity.z = 0;
			}
			
			// Yeni pozisyonu hesapla (x = x0 + v*t)
			Vector3f newPos = new Vector3f(
				position.x + velocity.x * delta,
				position.y + velocity.y * delta,
				position.z + velocity.z * delta
			);
			
			// Basit Çarpışma Kontrolü (Collision Detection)
			boolean collided = false;
			Collider myCollider = physics.getCollider();
			
			if (myCollider != null) {
				// Diğer tüm objelerle çarpışmayı test et (Bu O(N^2) basit bir yöntemdir, ileride Octree vs. eklenebilir)
				for (Entity otherEntity : entities) {
					if (entity == otherEntity) continue; // Kendinle çarpışamazsın
					
					PhysicsComponent otherPhysics = otherEntity.getPhysicsComponent();
					if (otherPhysics != null && otherPhysics.getCollider() != null) {
						// Eğer yeni pozisyona giderse çarpışacak mı?
						if (myCollider.intersects(otherPhysics.getCollider(), newPos, otherEntity.getPosition())) {
							collided = true;
							resolveCollision(entity, otherEntity, physics, otherPhysics);
							break; // Çarpıştıysa döngüden çık
						}
					}
				}
			}
			
			// Çarpışma yoksa konumu uygula
			if (!collided) {
				entity.setPosition(newPos);
			}
		}
	}

	/**
	 * İki obje birbirine çarptığında fiziksel tepkiyi (örneğin durma, sekme) hesaplar.
	 */
	private void resolveCollision(Entity obj1, Entity obj2, PhysicsComponent phys1, PhysicsComponent phys2) {
		// Çarpışma çözümü (Basit durma mekanizması)
		// Şimdilik sadece objeyi durduruyoruz. Daha gelişmiş kütle aktarımları (Impulse) buraya yazılabilir.
		
		Vector3f vel1 = phys1.getVelocity();
		
		if (phys1.getBounciness() > 0) {
			// Y yönünde sekiyorsa hızını ters çevir ve sekme katsayısı ile azalt
			vel1.y = -vel1.y * phys1.getBounciness();
			vel1.x = vel1.x * phys1.getBounciness();
			vel1.z = vel1.z * phys1.getBounciness();
		} else {
			phys1.stop();
		}
	}

	/** @return Mevcut global yerçekimi vektörünü döndürür (Örn: Dünya=0,-9.81,0 / Uzay=0,0,0) */
	public Vector3f getGlobalGravity() {
		return globalGravity;
	}

	/**
	 * Global yerçekimi vektörünü değiştirir.
	 * @param globalGravity Yeni yerçekimi yönü ve şiddeti (Örn: uzay için new Vector3f(0,0,0))
	 */
	public void setGlobalGravity(Vector3f globalGravity) {
		this.globalGravity.set(globalGravity);
	}

	/** @return Hava sürtünme katsayısını döndürür (0=uzay boşluğu, 1=anında durur) */
	public float getAirDrag() {
		return airDrag;
	}

	/**
	 * Hava sürtünme katsayısını atar.
	 * @param airDrag 0.0 (yok) ile 1.0 (tam fren) arasında bir değer
	 */
	public void setAirDrag(float airDrag) {
		this.airDrag = airDrag;
	}

}
