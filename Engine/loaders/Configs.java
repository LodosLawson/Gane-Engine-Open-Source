package loaders;

/**
 * Oyun içerisindeki bir modelin veya varlığın özellik ayarlarını (şeffaflık, gölge vb.) tutan sınıf.
 */
public class Configs {
	
	// Ekstra harita (extra map - normal, specular vb.) kullanıp kullanmayacağı
	private boolean hasExtraMap = false;
	// Şeffaflık (transparency) içerip içermediği
	private boolean hasTransparency = false;
	// Yansıma (reflection) özelliğinin aktif olup olmadığı
	private boolean hasReflection = false;
	// Kırılma (refraction - örn. su altında görünüm) özelliğinin aktif olup olmadığı
	private boolean hasRefraction = false;
	// Modelin gölge oluşturup oluşturmayacağı
	private boolean castsShadow = false;
	// Önemli bir obje olup olmadığı (render önceliği veya efekti için)
	private boolean important = false;
	
	/** Ekstra harita durumunu belirler. */
	protected void setExtraMap(boolean hasExtraMap){
		this.hasExtraMap = hasExtraMap;
	}
	
	/** Şeffaflık durumunu belirler. */
	protected void setTransparency(boolean transparent){
		this.hasTransparency = transparent;
	}
	
	/** Yansıma durumunu belirler. */
	protected void setReflection(boolean hasReflection){
		this.hasReflection = hasReflection;
	}
	
	/** Kırılma (su vb.) durumunu belirler. */
	protected void setRefraction(boolean hasRefraction){
		this.hasRefraction = hasRefraction;
	}
	
	/** Gölge düşürme durumunu belirler. */
	protected void setCastsShadow(boolean shadow){
		this.castsShadow = shadow;
	}
	
	/** Önemlilik durumunu belirler. */
	protected void setImportant(boolean important){
		this.important = important;
	}

	/** @return Ekstra harita var mı? */
	protected boolean hasExtraMap() {
		return hasExtraMap;
	}

	/** @return Şeffaflık var mı? */
	protected boolean hasTransparency() {
		return hasTransparency;
	}
	
	/** @return Önemli bir obje mi? */
	protected boolean isImportant() {
		return important;
	}

	/** @return Yansıma var mı? */
	protected boolean hasReflection() {
		return hasReflection;
	}

	/** @return Kırılma var mı? */
	protected boolean hasRefraction() {
		return hasRefraction;
	}

	/** @return Gölge düşürüyor mu? */
	protected boolean castsShadow() {
		return castsShadow;
	} 

}
