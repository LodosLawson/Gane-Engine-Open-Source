package water;

/**
 * Dünyadaki belli bir alana yerleştirilen su hacmini temsil eder.
 * Yüzey dalgalanması, derinlik, renk, saydamlık ve sualtı efektleri
 * dahil tüm su özellikleri bu sınıftan kontrol edilir.
 */
public class WaterTile {

	// Her bir su fayansının temel boyutu (Genişlik ve Derinlik)
	public static final float TILE_SIZE = 10;

	private float height; // Suyun Y eksenindeki yüksekliği (Yüzey seviyesi)
	private float x, z; // Suyun Dünya (World) üzerindeki konumu
	private float size; // Suyun boyutu

	// --- Dalgalanma Ayarları ---
	private float waveSpeed = 0.0005f;       // Dalga akış hızı
	private float waveStrength = 0.01f;      // Dalga bükülme (distortion) gücü
	private float waveAmplitude = 0.15f;     // Vertex dalga yüksekliği (3D dalga efekti)
	private float waveFrequency = 0.3f;      // Dalga frekansı (sıklığı)
	private float moveFactor = 0f;

	// --- Renk ve Saydamlık ---
	private float colorR = 0.0f;   // Su rengi - Kırmızı
	private float colorG = 0.25f;  // Su rengi - Yeşil
	private float colorB = 0.4f;   // Su rengi - Mavi
	private float transparency = 0.35f; // Su saydamlığı (0=tam saydam, 1=tamamen opak)
	private float colorMixFactor = 0.15f; // Su renginin yoğunluğu

	// --- Derinlik Ayarları ---
	private float depth = 10.0f;  // Suyun derinliği (Alt kısmı ne kadar aşağıda)
	private float depthDarkness = 0.7f; // Alt kısımdaki koyuluk miktarı (0=açık, 1=kapkaranlık)

	// --- Sualtı Sis Efekti (Kamera su altına girdiğinde) ---
	private float underwaterFogDensity = 0.04f; // Sualtı sis yoğunluğu
	private float underwaterFogR = 0.0f;  // Sualtı sis rengi - Kırmızı
	private float underwaterFogG = 0.15f; // Sualtı sis rengi - Yeşil
	private float underwaterFogB = 0.3f;  // Sualtı sis rengi - Mavi

	// --- Fresnel & Specular ---
	private float fresnelPower = 0.6f;  // Fresnel etkisi gücü (açıya göre yansıma)
	private float shineDamper = 30.0f;  // Specular ışık keskinliği
	private float reflectivity = 0.9f;  // Yüzey yansıtma gücü

	/**
	 * Yeni bir su fayansı oluşturur (varsayılan boyut).
	 */
	public WaterTile(float centerX, float centerZ, float height) {
		this(centerX, centerZ, height, TILE_SIZE);
	}

	/**
	 * Özel boyutta yeni bir su fayansı oluşturur.
	 */
	public WaterTile(float centerX, float centerZ, float height, float size) {
		this.x = centerX;
		this.z = centerZ;
		this.height = height;
		this.size = size;
	}

	// ==================== Konum & Boyut ====================

	public float getHeight() { return height; }
	public void setHeight(float height) { this.height = height; }

	public float getX() { return x; }
	public void setX(float x) { this.x = x; }

	public float getZ() { return z; }
	public void setZ(float z) { this.z = z; }

	public float getSize() { return size; }
	public void setSize(float size) { this.size = size; }

	// ==================== Dalgalanma ====================

	public float getWaveSpeed() { return waveSpeed; }
	public void setWaveSpeed(float waveSpeed) { this.waveSpeed = waveSpeed; }

	public float getWaveStrength() { return waveStrength; }
	public void setWaveStrength(float waveStrength) { this.waveStrength = waveStrength; }

	public float getWaveAmplitude() { return waveAmplitude; }
	/** Vertex dalga yüksekliğini ayarlar (0=düz, 0.5=belirgin dalgalar) */
	public void setWaveAmplitude(float waveAmplitude) { this.waveAmplitude = waveAmplitude; }

	public float getWaveFrequency() { return waveFrequency; }
	/** Dalga frekansını (sıklık) ayarlar (küçük değer=geniş dalgalar, büyük değer=dar dalgalar) */
	public void setWaveFrequency(float waveFrequency) { this.waveFrequency = waveFrequency; }

	public float getMoveFactor() { return moveFactor; }
	public void setMoveFactor(float moveFactor) { this.moveFactor = moveFactor; }

	/** Dalgaların akışını günceller (Her karede çağrılır) */
	public void updateMoveFactor() {
		this.moveFactor = (this.moveFactor + this.waveSpeed) % 1f;
	}

	// ==================== Renk & Saydamlık ====================

	/** Su rengini ayarlar (RGB: 0.0-1.0) */
	public void setWaterColor(float r, float g, float b) {
		this.colorR = r; this.colorG = g; this.colorB = b;
	}
	public float getColorR() { return colorR; }
	public float getColorG() { return colorG; }
	public float getColorB() { return colorB; }

	public float getTransparency() { return transparency; }
	/** Saydamlığı ayarlar (0=tam saydam / cam gibi, 1=opak / koyu su) */
	public void setTransparency(float transparency) { this.transparency = transparency; }

	public float getColorMixFactor() { return colorMixFactor; }
	/** Su renginin arka planla karışım oranını ayarlar (0=renksiz, 1=tam renkli su) */
	public void setColorMixFactor(float factor) { this.colorMixFactor = factor; }

	// ==================== Derinlik ====================

	public float getDepth() { return depth; }
	/** Suyun derinliğini ayarlar (Alt yüzeye olan mesafe) */
	public void setDepth(float depth) { this.depth = depth; }

	public float getDepthDarkness() { return depthDarkness; }
	/** Suyun dibinin koyuluk miktarını ayarlar (0=aydınlık, 1=kapkaranlık) */
	public void setDepthDarkness(float darkness) { this.depthDarkness = darkness; }

	// ==================== Sualtı Sis Efekti ====================

	public float getUnderwaterFogDensity() { return underwaterFogDensity; }
	/** Sualtı sis yoğunluğunu ayarlar */
	public void setUnderwaterFogDensity(float density) { this.underwaterFogDensity = density; }

	/** Sualtı sis rengini ayarlar (RGB: 0.0-1.0) */
	public void setUnderwaterFogColor(float r, float g, float b) {
		this.underwaterFogR = r; this.underwaterFogG = g; this.underwaterFogB = b;
	}
	public float getUnderwaterFogR() { return underwaterFogR; }
	public float getUnderwaterFogG() { return underwaterFogG; }
	public float getUnderwaterFogB() { return underwaterFogB; }

	// ==================== Fresnel & Specular ====================

	public float getFresnelPower() { return fresnelPower; }
	/** Fresnel gücünü ayarlar (Yüzeyin bakış açısına göre yansıma oranı) */
	public void setFresnelPower(float power) { this.fresnelPower = power; }

	public float getShineDamper() { return shineDamper; }
	/** Specular ışık keskinliğini ayarlar */
	public void setShineDamper(float damper) { this.shineDamper = damper; }

	public float getReflectivity() { return reflectivity; }
	/** Yüzey yansıtma gücünü ayarlar */
	public void setReflectivity(float reflectivity) { this.reflectivity = reflectivity; }

}
