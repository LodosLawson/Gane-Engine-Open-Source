package physics;

import org.lwjgl.util.vector.Vector3f;

/**
 * Varlıklara (Entity) fiziksel özellikler (yerçekimi, hız, kütle, çarpışma sınırı vb.) kazandıran bileşen.
 */
public class PhysicsComponent {

	// Hız vektörü (Saniyede hareket ettiği miktar)
	private Vector3f velocity;
	// İvme vektörü
	private Vector3f acceleration;
	
	// Objenin kütlesi (Ağırlık hesaplamaları ve itme gücü için)
	private float mass;
	// Ortamın yerçekiminden ne kadar etkileneceği çarpanı (Örn: 1.0 tam etkilenir, 0.0 hiç etkilenmez, -1.0 yukarı düşer)
	private float gravityScale;
	// Sekme katsayısı (0.0 hiç sekmez, 1.0 enerjisini kaybetmeden zıplar)
	private float bounciness;
	
	// Obje sabit bir nesne mi? (Zemin, duvar vb. fizik kuvvetlerinden etkilenmez ama diğerlerine çarpar)
	private boolean isStatic;
	// Sadece 2D boyutta mı hareket etsin? (Z ekseni yok sayılır)
	private boolean is2D;
	
	// Fiziksel sınırları (Kutu, küre vb.)
	private Collider collider;

	public PhysicsComponent() {
		this.velocity = new Vector3f(0, 0, 0);
		this.acceleration = new Vector3f(0, 0, 0);
		this.mass = 1.0f;
		this.gravityScale = 1.0f;
		this.bounciness = 0.0f;
		this.isStatic = false;
		this.is2D = false;
	}

	/** @return Objenin anlık hız vektörünü döndürür (x, y, z eksenleri) */
	public Vector3f getVelocity() {
		return velocity;
	}

	/** Objenin hız vektörünü doğrudan atar */
	public void setVelocity(Vector3f velocity) {
		this.velocity.set(velocity);
	}
	
	/** Mevcut hıza ek bir delta hız ekler (itmek, fırlatmak için kullanılır) */
	public void addVelocity(Vector3f deltaVel) {
		Vector3f.add(this.velocity, deltaVel, this.velocity);
	}

	/** @return Objenin anlık ivme vektörünü döndürür */
	public Vector3f getAcceleration() {
		return acceleration;
	}

	/** Objenin ivmesini doğrudan atar (Örn: motor kuvveti) */
	public void setAcceleration(Vector3f acceleration) {
		this.acceleration.set(acceleration);
	}

	/** @return Objenin kütlesini (kg) döndürür */
	public float getMass() {
		return mass;
	}

	/** Objenin kütlesini atar (Büyük kütle = daha az ivme etkisi) */
	public void setMass(float mass) {
		this.mass = mass;
	}

	/** @return Yerçekimi etki çarpanını döndürür (1.0=normal, 0=etkilenmez, -1=yukarı düşer) */
	public float getGravityScale() {
		return gravityScale;
	}

	/** Objenin yerçekiminden ne kadar etkileneceğini belirler */
	public void setGravityScale(float gravityScale) {
		this.gravityScale = gravityScale;
	}

	/** @return Objenin sekme (zıplama) katsayısını döndürür (0=sekmez, 1=tam seker) */
	public float getBounciness() {
		return bounciness;
	}

	/** Objenin sekme katsayısını atar */
	public void setBounciness(float bounciness) {
		this.bounciness = bounciness;
	}

	/** @return Obje fiziksel kuvvetlerden etkilenmeyen sabit bir nesne mi? (Zemin, duvar vb.) */
	public boolean isStatic() {
		return isStatic;
	}

	/** Objenin sabit (Static) olup olmadığını belirler */
	public void setStatic(boolean isStatic) {
		this.isStatic = isStatic;
	}

	/** @return Obje yalnızca 2D (XY düzlemi) hareketini destekliyor mu? */
	public boolean is2D() {
		return is2D;
	}

	/** 2D modunu aktif veya pasif yapar (3D sahnede Z hareketini kilitler) */
	public void set2D(boolean is2D) {
		this.is2D = is2D;
	}

	/** @return Objeye atanmış çarpışma sınırını (Collider) döndürür, yoksa null */
	public Collider getCollider() {
		return collider;
	}

	/** Objeye yeni bir çarpışma sınırı (AABB, Sphere vb.) atar */
	public void setCollider(Collider collider) {
		this.collider = collider;
	}
	
	/** Objenin üzerindeki anlık tüm hız ve ivme güçlerini sıfırlar */
	public void stop() {
		this.velocity.set(0, 0, 0);
		this.acceleration.set(0, 0, 0);
	}

}
