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
	
	/**
	 * Bu kaplamaya ait tüm doku dosyalarını ekran kartı belleğinden siler.
	 */
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
	
}
