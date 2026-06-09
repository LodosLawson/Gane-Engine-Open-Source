package physics;

import java.util.List;
import org.lwjgl.util.vector.Vector3f;
import scene.Entity;
import scene.Scene;
import particles.ParticleManager;

/**
 * Sahnedeki tÃ¼m fiziksel nesnelerin etkileÅŸimini, yerÃ§ekimini ve Ã§arpÄ±ÅŸmalarÄ±nÄ± yÃ¶neten ana Fizik Motoru.
 * KullanÄ±cÄ± tarafÄ±ndan global uzay / dÃ¼nya ÅŸartlarÄ± (YerÃ§ekimi yÃ¶nÃ¼, sÃ¼rtÃ¼nme) buradan deÄŸiÅŸtirilebilir.
 */
public class PhysicsEngine {

	// OrtamÄ±n (DÃ¼nyanÄ±n) yerÃ§ekimi kuvveti. 
	// DÃ¼nya iÃ§in y ekseninde aÅŸaÄŸÄ±ya doÄŸrudur Ã¶rn: (0, -9.81f, 0)
	// Uzay ortamÄ± iÃ§in (0, 0, 0) yapÄ±labilir.
	private Vector3f globalGravity = new Vector3f(0, -9.81f, 0);
	
	// RÃ¼zgar HÄ±zÄ± ve YÃ¶nÃ¼ (Vector3f)
	// VarsayÄ±lan olarak X yÃ¶nÃ¼nde hafif bir rÃ¼zgar (Ã¶rnek: 5.0f, 0f, 2.0f)
	private Vector3f windVelocity = new Vector3f(5.0f, 0.0f, 2.0f);
	
	// Ortamdaki sÃ¼rtÃ¼nme katsayÄ±sÄ± (HavanÄ±n objeleri ne kadar yavaÅŸlattÄ±ÄŸÄ±).
	// 0.0 hiÃ§ sÃ¼rtÃ¼nme yok (Uzay boÅŸluÄŸu), 1.0 anÄ±nda durdurur.
	private float airDrag = 0.01f;
	
	// Uzay / Gezegen FiziÄŸi Modu
	private GravityMode gravityMode = GravityMode.DIRECTIONAL;
	
	// Gezegensel Ã§ekim merkezi (Sadece PLANETARY modunda kullanÄ±lÄ±r)
	private Vector3f planetaryCenter = new Vector3f(0, 0, 0);
	private float planetaryGravityStrength = 9.81f;

	public PhysicsEngine() {
		
	}

	/**
	 * Her oyun karesinde (frame) Ã§aÄŸrÄ±larak sahnedeki tÃ¼m fiziksel objeleri gÃ¼nceller.
	 * 
	 * @param scene Objelerin Ã§ekileceÄŸi sahne
	 * @param delta GeÃ§en zaman (Delta Time)
	 */
	public void update(Scene scene, float delta) {
		List<Entity> entities = scene.getAllEntities();
		
		// 1. AÅŸama: TÃ¼m dinamik objelere kuvvetleri (YerÃ§ekimi, hÄ±z) uygula
		for (Entity entity : entities) {
			PhysicsComponent physics = entity.getPhysicsComponent();
			
			// Objenin fiziÄŸi yoksa veya sabitse (duvar/zemin) hareket ettirme
			if (physics == null || physics.isStatic()) {
				continue;
			}
			
			Vector3f velocity = physics.getVelocity();
			Vector3f acceleration = physics.getAcceleration();
			Vector3f position = entity.getPosition();
			
			// YerÃ§ekimi moduna gÃ¶re kuvveti hesapla
			Vector3f gravityForce = new Vector3f(0, 0, 0);
			
			if (gravityMode == GravityMode.DIRECTIONAL) {
				gravityForce.set(
					globalGravity.x * physics.getGravityScale(),
					globalGravity.y * physics.getGravityScale(),
					globalGravity.z * physics.getGravityScale()
				);
			} else if (gravityMode == GravityMode.PLANETARY) {
				Vector3f dirToCenter = Vector3f.sub(planetaryCenter, position, null);
				float distSquared = dirToCenter.lengthSquared();
				if (distSquared > 0) {
					float dist = (float)Math.sqrt(distSquared);
					dirToCenter.scale(1f/dist); // normalize
					
					float strength = planetaryGravityStrength * physics.getGravityScale();
					
					// Uzay FiziÄŸi (Newton'un Evrensel KÃ¼tleÃ§ekim YasasÄ±)
					// Gezegenin yÃ¼zeyi/atmosferi 5000 birim.
					// Atmosferden (5000'den) uzaklaÅŸtÄ±kÃ§a yerÃ§ekimi 1/r^2 kuralÄ±na gÃ¶re hÄ±zla azalÄ±r!
					if (dist > 5000f) {
						strength = strength * ((5000f * 5000f) / distSquared);
					}
					
					gravityForce.set(dirToCenter.x * strength, dirToCenter.y * strength, dirToCenter.z * strength);
				}
			}
			// ZERO_GRAVITY modunda gravityForce zaten (0,0,0) olarak kalÄ±r
			
			// HÄ±za ivmeyi ve yerÃ§ekimini ekle (v = v0 + a*t)
			velocity.x += (acceleration.x + gravityForce.x) * delta;
			velocity.y += (acceleration.y + gravityForce.y) * delta;
			velocity.z += (acceleration.z + gravityForce.z) * delta;
			
			// Hava sÃ¼rtÃ¼nmesi (Drag) - HÄ±zÄ± yavaÅŸ yavaÅŸ keser
			velocity.x -= velocity.x * airDrag * delta;
			velocity.y -= velocity.y * airDrag * delta;
			velocity.z -= velocity.z * airDrag * delta;
			
			// --- SU FÄ°ZÄ°ÄžÄ° (BUOYANCY & DRAG & WIND) ---
			if (!scene.getWater().isEmpty()) {
				water.tile.WaterTile waterTile = scene.getWater().get(0);
				float waterHeight = waterTile.getWaterHeightAt(position.x, position.z);
				
				float objectHeight = 2.0f;
				if (physics.getCollider() instanceof AABB) {
					AABB aabb = (AABB) physics.getCollider();
					objectHeight = Math.max(aabb.getMaxOffset().y - aabb.getMinOffset().y, 0.5f);
				}
				
				float bottomY = position.y - objectHeight / 2.0f;
				float submergedRatio = 0.0f;
				if (waterHeight > bottomY) {
					submergedRatio = (waterHeight - bottomY) / objectHeight;
					if (submergedRatio > 1.0f) {
						submergedRatio = 1.0f;
					}
				}
				
				if (submergedRatio > 0.0f) {
					// 1. Physical Buoyancy (Archimedes' Principle):
					float WATER_DENSITY = 1.0f;
					
					// Calculate volume and waterlogged state
					float volume = physics.getVolume();
					if (physics.canBeWaterLogged()) {
						float currentRatio = physics.getWaterLoggedRatio();
						float newRatio = currentRatio + physics.getWaterLoggedRate() * submergedRatio * delta;
						physics.setWaterLoggedRatio(Math.min(newRatio, 1.0f));
					}
					
					float dryMass = physics.getMass();
					float waterMass = volume * physics.getWaterLoggedRatio() * WATER_DENSITY;
					float totalMass = dryMass + waterMass;
					
					// Displaced fluid mass
					float displacedMass = volume * submergedRatio * WATER_DENSITY;
					
					// Buoyancy scale is the ratio of displaced fluid mass to total mass
					float buoyancyScale = displacedMass / totalMass;
					
					// Gravity acceleration vector (defaulting to normal Earth gravity if none exists)
					float gX = gravityForce.x;
					float gY = gravityForce.y;
					float gZ = gravityForce.z;
					if (gX == 0f && gY == 0f && gZ == 0f) {
						gY = -9.81f;
					}
					
					// Buoyancy acceleration vector opposes gravity
					float bAccelX = -gX * buoyancyScale;
					float bAccelY = -gY * buoyancyScale;
					float bAccelZ = -gZ * buoyancyScale;
					
					velocity.x += bAccelX * delta;
					velocity.y += bAccelY * delta;
					velocity.z += bAccelZ * delta;
					
					// 2. AkÄ±ÅŸkan Direnci (Fluid Drag): Suda Ã§ok daha yÃ¼ksek sÃ¼rtÃ¼nme
					float waterDragLinear = 3.5f;
					float waterDragQuadratic = 0.7f;
					velocity.x -= (velocity.x * waterDragLinear + velocity.x * Math.abs(velocity.x) * waterDragQuadratic) * submergedRatio * delta;
					velocity.y -= (velocity.y * waterDragLinear + velocity.y * Math.abs(velocity.y) * waterDragQuadratic) * submergedRatio * delta;
					velocity.z -= (velocity.z * waterDragLinear + velocity.z * Math.abs(velocity.z) * waterDragQuadratic) * submergedRatio * delta;
					
					// 3. RÃ¼zgar SÃ¼rÃ¼klemesi: RÃ¼zgarÄ±n su Ã¼stÃ¼ndeki kÄ±sma etkisi
					Vector3f wind = scene.getWindVelocity();
					float windPushFactor = 0.08f * (1.0f - submergedRatio);
					velocity.x += wind.x * windPushFactor * delta;
					velocity.z += wind.z * windPushFactor * delta;
					
					// 4. SÃ¼rÃ¼klenme esnasÄ±nda kÃ¶pÃ¼k partikÃ¼lÃ¼ Ã¼retimi
					float speedHoriz = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
					if (speedHoriz > 1.2f && Math.random() < 0.25) {
						ParticleManager.getInstance().spawnFoam(position.x, waterHeight, position.z, 0.45f);
					}
				}
			}
			
			// EÄŸer 2D bir obje ise Z eksenindeki hareketleri iptal et
			if (physics.is2D()) {
				velocity.z = 0;
			}
			// Yeni pozisyonu hesapla (x = x0 + v*t)
			Vector3f newPos = new Vector3f(
				position.x + velocity.x * delta,
				position.y + velocity.y * delta,
				position.z + velocity.z * delta
			);

			// Arazi Ã‡arpÄ±ÅŸma KontrolÃ¼ (Terrain Collision for physics entities)
			if (!scene.getTerrains().isEmpty()) {
				terrain.ITerrain terrainObj = scene.getTerrains().get(0);
				float groundHeight = terrainObj.getHeightAt(newPos.x, newPos.z);
				
				float bottomY = newPos.y;
				if (physics.getCollider() instanceof AABB) {
					AABB aabb = (AABB) physics.getCollider();
					bottomY = newPos.y + aabb.getMinOffset().y;
				}
				
				if (bottomY < groundHeight) {
					float offset = groundHeight - bottomY;
					newPos.y += offset;
					
					if (physics.getBounciness() > 0.0f) {
						velocity.y = -velocity.y * physics.getBounciness();
					} else {
						velocity.y = 0.0f;
					}
					velocity.x *= 0.8f;
					velocity.z *= 0.8f;
				}
			}

			// Basit Ã‡arpÄ±ÅŸma KontrolÃ¼ (Collision Detection)
			boolean collided = false;
			Collider myCollider = physics.getCollider();
			
			if (myCollider != null) {
				// DiÄŸer tÃ¼m objelerle Ã§arpÄ±ÅŸmayÄ± test et (Bu O(N^2) basit bir yÃ¶ntemdir, ileride Octree vs. eklenebilir)
				for (Entity otherEntity : entities) {
					if (entity == otherEntity) continue; // Kendinle Ã§arpÄ±ÅŸamazsÄ±n
					
					PhysicsComponent otherPhysics = otherEntity.getPhysicsComponent();
					if (otherPhysics != null && otherPhysics.getCollider() != null) {
						// EÄŸer yeni pozisyona giderse Ã§arpÄ±ÅŸacak mÄ±?
						if (myCollider.intersects(otherPhysics.getCollider(), newPos, otherEntity.getPosition())) {
							collided = true;
							resolveCollision(entity, otherEntity, physics, otherPhysics);
							break; // Ã‡arpÄ±ÅŸtÄ±ysa dÃ¶ngÃ¼den Ã§Ä±k
						}
					}
				}
			}
			
			// Ã‡arpÄ±ÅŸma yoksa konumu uygula
			if (!collided) {
				entity.setPosition(newPos);
			}
		}
	}

	/**
	 * Ä°ki obje birbirine Ã§arptÄ±ÄŸÄ±nda fiziksel tepkiyi (Ã¶rneÄŸin durma, sekme) hesaplar.
	 */
	private void resolveCollision(Entity obj1, Entity obj2, PhysicsComponent phys1, PhysicsComponent phys2) {
		// Ã‡arpÄ±ÅŸma Ã§Ã¶zÃ¼mÃ¼ (Basit durma mekanizmasÄ±)
		// Åžimdilik sadece objeyi durduruyoruz. Daha geliÅŸmiÅŸ kÃ¼tle aktarÄ±mlarÄ± (Impulse) buraya yazÄ±labilir.
		
		Vector3f vel1 = phys1.getVelocity();
		
		if (phys1.getBounciness() > 0) {
			// Y yÃ¶nÃ¼nde sekiyorsa hÄ±zÄ±nÄ± ters Ã§evir ve sekme katsayÄ±sÄ± ile azalt
			vel1.y = -vel1.y * phys1.getBounciness();
			vel1.x = vel1.x * phys1.getBounciness();
			vel1.z = vel1.z * phys1.getBounciness();
		} else {
			phys1.stop();
		}
	}

	/** @return Mevcut global yerÃ§ekimi vektÃ¶rÃ¼nÃ¼ dÃ¶ndÃ¼rÃ¼r (Ã–rn: DÃ¼nya=0,-9.81,0 / Uzay=0,0,0) */
	public Vector3f getGlobalGravity() {
		return globalGravity;
	}

	/**
	 * Global yerÃ§ekimi vektÃ¶rÃ¼nÃ¼ deÄŸiÅŸtirir.
	 * @param globalGravity Yeni yerÃ§ekimi yÃ¶nÃ¼ ve ÅŸiddeti (Ã–rn: uzay iÃ§in new Vector3f(0,0,0))
	 */
	public void setGlobalGravity(Vector3f globalGravity) {
		this.globalGravity.set(globalGravity);
	}

	/** @return Hava sÃ¼rtÃ¼nme katsayÄ±sÄ±nÄ± dÃ¶ndÃ¼rÃ¼r (0=uzay boÅŸluÄŸu, 1=anÄ±nda durur) */
	public float getAirDrag() {
		return airDrag;
	}

	/**
	 * Hava sÃ¼rtÃ¼nme katsayÄ±sÄ±nÄ± atar.
	 * @param airDrag 0.0 (yok) ile 1.0 (tam fren) arasÄ±nda bir deÄŸer
	 */
	public void setAirDrag(float airDrag) {
		this.airDrag = airDrag;
	}

	public GravityMode getGravityMode() {
		return gravityMode;
	}

	public void setGravityMode(GravityMode gravityMode) {
		this.gravityMode = gravityMode;
	}

	public Vector3f getPlanetaryCenter() {
		return planetaryCenter;
	}

	public void setPlanetaryCenter(Vector3f planetaryCenter) {
		this.planetaryCenter.set(planetaryCenter);
	}

	public float getPlanetaryGravityStrength() {
		return planetaryGravityStrength;
	}

	public void setPlanetaryGravityStrength(float planetaryGravityStrength) {
		this.planetaryGravityStrength = planetaryGravityStrength;
	}

	public Vector3f getWindVelocity() {
		return windVelocity;
	}

	public void setWindVelocity(Vector3f windVelocity) {
		this.windVelocity.set(windVelocity);
	}

}

