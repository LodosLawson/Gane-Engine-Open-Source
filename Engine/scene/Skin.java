package scene;

import textures.Texture;

/**
 * Bir 3B objenin yüzey kaplamasını (materyalini) tanımlayan sınıf.
 * Difüz (renk) dokusunu, varsa ekstra bilgi dokularını (örn. parlaklık/normal haritası)
 * ve transparanlık gibi materyal özelliklerini barındırır.
 */
public class Skin {

	// Objeye asıl rengini ve desenini veren temel kaplama (Diffuse Map)
	private Texture diffuseTexture;
	// Işıklandırma için ekstra bilgiler içeren (örn. RGB kanallarında farklı değerler tutan) özel kaplama
	private Texture extraInfoMap;
	
	// Bu kaplamanın saydam/yarı saydam pikseller içerip içermediği
	private boolean transparent;
	
	// Objeye sahte ışıklandırma uygulanıp uygulanmayacağı (Örn: Çimenlerin normalini yukarı sabitleme)
	private boolean useFakeLighting = false;
	
	// Arka yüz gizleme (back-face culling) aktif mi? Varsayılan olarak aktiftir.
	private boolean cullBackFaces = true;
	
	// Doku atlasındaki (Texture Atlas) satır/sütun sayısı (Varsayılan 1)
	private int numberOfRows = 1;
	
	/**
	 * Yeni bir materyal (Skin) oluşturur.
	 * 
	 * @param diffuseTexture Ana renk kaplaması
	 * @param extraInfoMap Ekstra bilgi kaplaması (yoksa null olabilir)
	 */
	public Skin(Texture diffuseTexture, Texture extraInfoMap){
		this.diffuseTexture = diffuseTexture;
		this.extraInfoMap = extraInfoMap;
	}
	
	/** Kaplamanın tüm doku dosyalarını ekran kartı belleğinden siler. */
	public void delete(){
		diffuseTexture.delete();
		if(extraInfoMap!=null){
			extraInfoMap.delete();
		}
	}
	
	/** Kaplamanın transparan (saydam) olup olmadığını ayarlar */
	public void setTransparent(boolean transparent){
		this.transparent = transparent;
	}
	
	/** Arka yüz gizlemeyi (Back-face culling) ayarlar */
	public void setCullBackFaces(boolean cullBackFaces) {
		this.cullBackFaces = cullBackFaces;
	}
	
	/** @return Arka yüz gizleme aktif mi? */
	public boolean isCullBackFaces() {
		return cullBackFaces;
	}
	
	/** Objeye sahte ışıklandırma uygulanmasını ayarlar */
	public void setUseFakeLighting(boolean useFakeLighting) {
		this.useFakeLighting = useFakeLighting;
	}
	
	/** @return Objeye sahte ışık uygulanıyor mu? */
	public boolean isUseFakeLighting() {
		return useFakeLighting;
	}
	
	/** @return Kaplama saydam pikseller içeriyor mu? (Örn. Yaprak, Cam) */
	public boolean hasTransparency(){
		return transparent;
	}
	
	/** @return Bu objenin özel bir ekstra haritası (Extra Info Map) var mı? */
	public boolean hasExtraMap(){
		return extraInfoMap!=null;
	}
	
	/** @return Objeye renk veren ana kaplamayı (Diffuse) döndürür */
	public Texture getDiffuseTexture(){
		return diffuseTexture;
	}
	
	/** @return Objeye ekstra detay veren haritayı döndürür */
	public Texture getExtraInfoMap(){
		return extraInfoMap;
	}

	/** @return Doku atlasındaki satır sayısını döndürür */
	public int getNumberOfRows() {
		return numberOfRows;
	}

	/**
	 * Doku atlasındaki satır/sütun sayısını ayarlar.
	 * Örneğin 4x4 bir atlas için 4 girilmelidir.
	 */
	public void setNumberOfRows(int numberOfRows) {
		this.numberOfRows = numberOfRows;
	}
}
