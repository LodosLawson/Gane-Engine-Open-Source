package scene;

import org.lwjgl.util.vector.Vector3f;

/**
 * Sahnedeki temel bir nesneyi temsil eder.
 * Bir objenin modeli, materyali (kaplaması), konumu ve ışıkla/suyla olan
 * etkileşim özelliklerini (gölge, yansıma vb.) tutar.
 */
public class Entity {
	
	// Objenin geometrik şekli (VAO)
	private final Model model;
	// Objenin dış görünüşü (Dokular/Texture)
	private final Skin skin;
	// Objenin 3B uzaydaki konumu
	private final Vector3f position = new Vector3f(0, 0, 0);
	
	// Fizik motoru ile etkileşimi sağlayacak bileşen (Yerçekimi, hız, kütle vb.)
	private physics.PhysicsComponent physicsComponent;
	
	// Bu obje gölge oluşturur mu? (Varsayılan: Evet)
	private boolean castsShadow = true;
	// Su gibi yüzeylerde yansır mı? (Varsayılan: Evet)
	private boolean hasReflection = true;
	// Su altında kırılma efektinde gözükür mü? (Varsayılan: Hayır)
	private boolean seenUnderWater = false;
	// Düşük kaliteli yansıma sahnelerinde vs. çizilecek kadar önemli mi?
	private boolean isImportant = false;
	
	/**
	 * Yeni bir obje oluşturur.
	 * 
	 * @param model Objenin geometrik modeli
	 * @param skin Objenin doku seti (Materyal)
	 */
	public Entity(Model model, Skin skin){
		this.model = model;
		this.skin = skin;
	}

	/** @return Objenin geometrik modelini döndürür */
	public Model getModel() {
		return model;
	}

	/** @return Objenin kaplama özelliklerini döndürür */
	public Skin getSkin() {
		return skin;
	}
	
	/** @return Objenin şu anki pozisyonunu döndürür */
	public Vector3f getPosition() {
		return position;
	}

	/**
	 * Objenin uzaydaki konumunu değiştirir.
	 * 
	 * @param position Yeni x,y,z koordinatları
	 */
	public void setPosition(Vector3f position) {
		this.position.set(position);
	}
	
	/** Objenin barındırdığı modeli ve dokuyu ekran kartından siler */
	public void delete(){
		model.delete();
		skin.delete();
	}

	/**
	 * Objenin her karede çalışacak mantıksal döngüsü (Animasyon vs. için).
	 * Standart nesnelerin varsayılan davranışı yoktur (boş bırakılmıştır).
	 * 
	 * @param delta Geçen zaman (saniye vb.)
	 */
	public void update(float delta) {
		// Default entities have no update behavior.
	}

	/** @return Bu nesne ışığa karşı gölge üretiyor mu? */
	public boolean isShadowCasting() {
		return castsShadow;
	}

	/** Nesnenin gölge üretip üretmeyeceğini ayarlar */
	public void setCastsShadow(boolean shadow) {
		this.castsShadow = shadow;
	}
	
	/** @return Nesne her zaman çizilmesi gereken önemli bir nesne mi? */
	public boolean isImportant(){
		return isImportant;
	}

	/** @return Nesnenin su yüzeyinde yansıması görünüyor mu? */
	public boolean hasReflection() {
		return hasReflection;
	}

	/** Nesnenin suda yansıyıp yansımayacağını ayarlar */
	public void setHasReflection(boolean reflects) {
		this.hasReflection = reflects;
	}
	
	/** Nesnenin önemli (her aşamada çizilecek) olup olmadığını ayarlar */
	public void setImportant(boolean isImportant) {
		this.isImportant = isImportant;
	}

	/** @return Suyun içinden dışarı doğru bakarken kırılma efektinde bu nesne görünüyor mu? */
	public boolean isSeenUnderWater() {
		return seenUnderWater;
	}

	/** Nesnenin suyun altındayken görünüp görünmeyeceğini ayarlar */
	public void setSeenUnderWater(boolean seenUnderWater) {
		this.seenUnderWater = seenUnderWater;
	}

	/** @return Objenin üzerinde tanımlı olan fizik bileşenini döndürür */
	public physics.PhysicsComponent getPhysicsComponent() {
		return physicsComponent;
	}

	/** Objenin fizik bileşenini atar (Böylece fizik motoruna dahil olur) */
	public void setPhysicsComponent(physics.PhysicsComponent physicsComponent) {
		this.physicsComponent = physicsComponent;
	}

	/** @return Objenin bir fizik bileşeni var mı? */
	public boolean hasPhysics() {
		return physicsComponent != null;
	}

}
