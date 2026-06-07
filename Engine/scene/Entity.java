package scene;

import org.lwjgl.util.vector.Vector3f;

/**
 * Sahnedeki temel bir nesneyi temsil eder.
 * Bir objenin modeli, materyali (kaplaması), konumu ve ışıkla/suyla olan
 * etkileşim özelliklerini (gölge, yansıma vb.) tutar.
 */
public class Entity {
	
	// Objenin geometrik şekli (VAO)
	private Model model;
	// Objenin dış görünüşü (Dokular/Texture)
	private Skin skin;
	// Objenin 3B uzaydaki konumu
	private final Vector3f position = new Vector3f(0, 0, 0);
	// Objenin 3B uzaydaki dönme açısı (Derece cinsinden X, Y, Z)
	private final Vector3f rotation = new Vector3f(0, 0, 0);
	// Objenin ölçek çarpanı (Boyutu)
	private float scale = 1.0f;
	// Modelin kendi eksenindeki varsayılan düzeltme rotasyonu (GLB dosyalarındaki Z-up to Y-up düzeltmesi için)
	private final Vector3f modelOffsetRot = new Vector3f(0, 0, 0);
	// Modelin merkez pivotunu ve taban noktasını sıfırlamak için kullanılan raw offset
	private final Vector3f baseOffset = new Vector3f(0, 0, 0);
	// Frustum Culling için objenin kapsama yarıçapı
	private float cullingRadius = 10.0f;
	
	// Occlusion Culling için görünürlük bayrağı ve GPU sorgusu
	private boolean visible = true;
	private openglObjects.Query occlusionQuery;
	
	// LOD Modelleri (Kamera uzaklaştıkça poligonu düşen modeller)
	private Model lod1Model; // Orta Mesafe
	private Model lod2Model; // Uzak Mesafe
	private float lod1Distance = 250.0f; // LOD1 modeline geçiş mesafesi
	private float lod2Distance = 600.0f; // LOD2 modeline geçiş mesafesi
	
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
	
	// Doku atlasındaki (Texture Atlas) hangi hücrenin kullanılacağı indeksi
	private int textureIndex = 0;
	
	// Çarpışma kutusu
	private physics.AABB boundingBox;
	
	// Kokpit / FPS Kamera Modu için offset konumu (X, Y, Z)
	private final Vector3f firstPersonOffset = new Vector3f(0, 4.8f, 0);

	public Vector3f getFirstPersonOffset() {
		return firstPersonOffset;
	}

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

	/** Objenin modelini dinamik olarak değiştirir (Anahtar kare animasyonları için) */
	public void setModel(Model model) {
		this.model = model;
	}

	/** @return Objenin kaplama özelliklerini döndürür */
	public Skin getSkin() {
		return skin;
	}

	/** Objenin kaplamasını dinamik olarak değiştirir */
	public void setSkin(Skin skin) {
		this.skin = skin;
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
	public void setPosition(org.lwjgl.util.vector.Vector3f position) {
		this.position.set(position);
	}

	/** @return Objenin şu anki dönme açısını (X, Y, Z derece) döndürür */
	public Vector3f getRotation() {
		return rotation;
	}

	/**
	 * Objenin uzaydaki dönme açısını değiştirir.
	 * 
	 * @param rotation Yeni x,y,z dönme açıları
	 */
	public void setRotation(Vector3f rotation) {
		this.rotation.set(rotation);
	}

	public Vector3f getModelOffsetRot() {
		return modelOffsetRot;
	}

	public Vector3f getBaseOffset() {
		return baseOffset;
	}

	/** @return Objenin ölçek çarpanını (Boyutunu) döndürür */
	public float getScale() {
		return scale;
	}

	/**
	 * Objenin boyut ölçeğini değiştirir.
	 * 
	 * @param scale Yeni ölçek çarpanı
	 */
	public void setScale(float scale) {
		this.scale = scale;
	}
	
	public physics.AABB getBoundingBox() {
		return boundingBox;
	}
	
	public void setBoundingBox(physics.AABB boundingBox) {
		this.boundingBox = boundingBox;
	}
	
	/** @return Frustum Culling için objenin kapsama yarıçapını döndürür */
	public float getCullingRadius() {
		return cullingRadius * scale;
	}

	/**
	 * Frustum Culling için objenin kapsama yarıçapını ayarlar.
	 * 
	 * @param cullingRadius Yeni kapsama yarıçapı
	 */
	public void setCullingRadius(float cullingRadius) {
		this.cullingRadius = cullingRadius;
	}
	
	public void setLodModels(Model lod1, Model lod2) {
		this.lod1Model = lod1;
		this.lod2Model = lod2;
	}

	public void setLodDistances(float lod1Dist, float lod2Dist) {
		this.lod1Distance = lod1Dist;
		this.lod2Distance = lod2Dist;
	}
	
	public void setLod2Distance(float lod2Distance) {
		this.lod2Distance = lod2Distance;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public openglObjects.Query getOcclusionQuery() {
		return occlusionQuery;
	}

	public void setOcclusionQuery(openglObjects.Query occlusionQuery) {
		this.occlusionQuery = occlusionQuery;
	}
	
	public Model getLod1Model() { return lod1Model; }
	public Model getLod2Model() { return lod2Model; }
	public float getLod1Distance() { return lod1Distance; }
	public float getLod2Distance() { return lod2Distance; }

	/** Objenin barındırdığı modeli ve dokuyu ekran kartından siler */
	public void delete(){
		if (model != null) model.delete();
		if (skin != null) skin.delete();
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

	public int getTextureIndex() {
		return textureIndex;
	}

	public void setTextureIndex(int textureIndex) {
		this.textureIndex = textureIndex;
	}

	public float getTextureXOffset() {
		int column = textureIndex % skin.getNumberOfRows();
		return (float) column / (float) skin.getNumberOfRows();
	}

	public float getTextureYOffset() {
		int row = textureIndex / skin.getNumberOfRows();
		return (float) row / (float) skin.getNumberOfRows();
	}

	private org.lwjgl.util.vector.Matrix4f[] jointTransforms;

	public org.lwjgl.util.vector.Matrix4f[] getJointTransforms() {
		return jointTransforms;
	}

	public void setJointTransforms(org.lwjgl.util.vector.Matrix4f[] jointTransforms) {
		this.jointTransforms = jointTransforms;
	}
}
