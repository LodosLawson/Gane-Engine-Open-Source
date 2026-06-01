package water.tile;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Okyanus / Su yÃ¼zeyi konfigÃ¼rasyon sÄ±nÄ±fÄ±.
 * 
 * KullanÄ±cÄ± bu sÄ±nÄ±f Ã¼zerinden tÃ¼m okyanus gÃ¶rÃ¼nÃ¼mÃ¼nÃ¼ Ã¶zelleÅŸtirebilir:
 *   Renk, saydamlÄ±k, dalga hÄ±zÄ±, dalga yÃ¼ksekliÄŸi, kÃ¶pÃ¼k, rÃ¼zgar yÃ¶nÃ¼...
 *
 * Ã–rnek kullanÄ±m (MainApp iÃ§inde):
 *   WaterTile ocean = new WaterTile(0, 0, -2.0f, 100f);
 *   ocean.setBaseColor(0.01f, 0.22f, 0.36f);    // Koyu lacivert okyanus
 *   ocean.setWaveHeight(1.5f);                   // Dalga yÃ¼ksekliÄŸi
 *   ocean.setWindSpeed(8.0f);                    // RÃ¼zgar hÄ±zÄ±
 *   ocean.setWindDirection(-0.4f, -0.9f);        // RÃ¼zgar yÃ¶nÃ¼ (normalize edilmeli)
 *   ocean.setFoamIntensity(0.6f);               // KÃ¶pÃ¼k yoÄŸunluÄŸu
 *   ocean.setFresnelPower(3.0f);                // Fresnel (kenar parlamasÄ±)
 *   ocean.setSpecularPower(128.0f);             // GÃ¼neÅŸ yansÄ±masÄ± keskinliÄŸi
 *   ocean.setDeepColor(0.0f, 0.05f, 0.12f);    // Derin su rengi
 *   activeScene.getWater().add(ocean);
 */
public class WaterTile {
	
	public static float TILE_SIZE = 100f;
	
	private float x, z;
	private float height;
	private float size;
	
	private float time = 0.0f;
	private float moveSpeed = 1.0f; // ZamanÄ±n Ã§arpanÄ±

	// --- RENK VE GÃ–RÃœNÃœM ---
	/** Suyun ana/temel rengi (yÃ¼zey rengi) */
	private Vector3f baseColor = new Vector3f(0.0123f, 0.3613f, 0.6867f);
	/** Derin sularda gÃ¶rÃ¼nen koyu renk tonu */
	private Vector3f deepColor = new Vector3f(0.005f, 0.03f, 0.08f);
	/** Su saydamlÄ±ÄŸÄ±: 0.0=tamamen saydam, 1.0=opak */
	private float transparency = 0.88f;

	// --- DALGA FÄ°ZÄ°ÄžÄ° ---
	/** Dalga genliÄŸi (yÃ¼ksekliÄŸi): bÃ¼yÃ¼k deÄŸer = daha yÃ¼ksek dalgalar */
	private float waveHeight = 0.45e-3f;
	/** RÃ¼zgar hÄ±zÄ± (m/s): bÃ¼yÃ¼k deÄŸer = daha uzun ve kuvvetli dalgalar */
	private float windSpeed = 6.5f;
	/** RÃ¼zgar yÃ¶nÃ¼ (normalize vektÃ¶r) */
	private Vector2f windDirection = new Vector2f(-0.4f, -0.9f);
	/** Dalga animasyon hÄ±zÄ± Ã§arpanÄ± */
	private float timeScale = 1.0f;

	// --- IÅžIK VE PARLAKLK ---
	/** GÃ¼neÅŸ yansÄ±masÄ±nÄ±n (specular) keskinliÄŸi: bÃ¼yÃ¼k deÄŸer = daha keskin/kÃ¼Ã§Ã¼k gÃ¼neÅŸ yolu */
	private float specularPower = 256.0f;
	/** GÃ¼neÅŸ yansÄ±masÄ±nÄ±n gÃ¼cÃ¼/parlaklÄ±ÄŸÄ± */
	private float specularIntensity = 1.2f;
	/** Fresnel kuvveti: yÃ¼zeyin kenarlarda ne kadar parlayacaÄŸÄ± */
	private float fresnelPower = 3.5f;
	/** Ortam Ä±ÅŸÄ±ÄŸÄ± (en karanlÄ±k gece tonu) */
	private Vector3f lightAmbient = new Vector3f(0.015f, 0.015f, 0.015f);

	// --- KÃ–PÃœK ---
	/** KÃ¶pÃ¼k yoÄŸunluÄŸu: 0.0=kÃ¶pÃ¼k yok, 1.0=maksimum kÃ¶pÃ¼k */
	private float foamIntensity = 0.4f;
	/** KÃ¶pÃ¼ÄŸÃ¼n gÃ¶rÃ¼ndÃ¼ÄŸÃ¼ dalga eÅŸiÄŸi */
	private float foamThreshold = 1.2f;

	// --- SU ALTI ---
	private float underwaterFogDensity = 0.03f;
	private float underwaterFogR = 0.0f;
	private float underwaterFogG = 0.25f;
	private float underwaterFogB = 0.45f;

	// --- DOKU ---
	private float textureScale = 0.6f;

	// ============================================================
	// CONSTRUCTORS
	// ============================================================

	public WaterTile(float centerX, float centerZ, float height, float size) {
		this.x = centerX;
		this.z = centerZ;
		this.height = height;
		this.size = size;
	}
	
	public WaterTile(float centerX, float centerZ, float height) {
		this(centerX, centerZ, height, TILE_SIZE);
	}
	
	public void update(float delta) {
		this.time += delta * moveSpeed;
	}

	// ============================================================
	// RENK VE GÃ–RÃœNÃœM
	// ============================================================

	public Vector3f getBaseColor() { return baseColor; }
	public void setBaseColor(Vector3f c) { this.baseColor.set(c); }
	public void setBaseColor(float r, float g, float b) { this.baseColor.set(r, g, b); }

	public Vector3f getDeepColor() { return deepColor; }
	public void setDeepColor(Vector3f c) { this.deepColor.set(c); }
	public void setDeepColor(float r, float g, float b) { this.deepColor.set(r, g, b); }

	public float getTransparency() { return transparency; }
	public void setTransparency(float transparency) { this.transparency = transparency; }

	// ============================================================
	// DALGA FÄ°ZÄ°ÄžÄ°
	// ============================================================

	/** Dalga yÃ¼ksekliÄŸi (varsayÄ±lan: 0.00045f). BÃ¼yÃ¼k deÄŸer = yÃ¼ksek dalgalar. */
	public float getWaveHeight() { return waveHeight; }
	public void setWaveHeight(float waveHeight) { this.waveHeight = waveHeight; }

	/** RÃ¼zgar hÄ±zÄ± m/s (varsayÄ±lan: 6.5). BÃ¼yÃ¼k deÄŸer = uzun/kuvvetli dalgalar. */
	public float getWindSpeed() { return windSpeed; }
	public void setWindSpeed(float windSpeed) { this.windSpeed = windSpeed; }

	/** RÃ¼zgar yÃ¶nÃ¼ (X, Z). Normalize edilmiÅŸ deÄŸer Ã¶nerilir. */
	public Vector2f getWindDirection() { return windDirection; }
	public void setWindDirection(float x, float z) { this.windDirection.set(x, z); }
	public void setWindDirection(Vector2f dir) { this.windDirection.set(dir); }

	/** Dalga animasyon hÄ±zÄ± Ã§arpanÄ± (varsayÄ±lan: 1.0). BÃ¼yÃ¼k deÄŸer = hÄ±zlÄ± dalgalanma. */
	public float getTimeScale() { return timeScale; }
	public void setTimeScale(float timeScale) { this.timeScale = timeScale; }

	public float getMoveSpeed() { return moveSpeed; }
	public void setMoveSpeed(float moveSpeed) { this.moveSpeed = moveSpeed; }

	// ============================================================
	// IÅžIK VE PARLAKLK
	// ============================================================

	/** GÃ¼neÅŸ yansÄ±masÄ± keskinliÄŸi (varsayÄ±lan: 256). BÃ¼yÃ¼k deÄŸer = ince, keskin gÃ¼neÅŸ yolu. */
	public float getSpecularPower() { return specularPower; }
	public void setSpecularPower(float specularPower) { this.specularPower = specularPower; }

	/** GÃ¼neÅŸ yansÄ±masÄ± gÃ¼cÃ¼ (varsayÄ±lan: 1.2). */
	public float getSpecularIntensity() { return specularIntensity; }
	public void setSpecularIntensity(float specularIntensity) { this.specularIntensity = specularIntensity; }

	/** Fresnel kuvveti (varsayÄ±lan: 3.5). BÃ¼yÃ¼k deÄŸer = kenarlarda daha fazla parlama. */
	public float getFresnelPower() { return fresnelPower; }
	public void setFresnelPower(float fresnelPower) { this.fresnelPower = fresnelPower; }

	public Vector3f getLightAmbient() { return lightAmbient; }
	public void setLightAmbient(Vector3f lightAmbient) { this.lightAmbient.set(lightAmbient); }

	// ============================================================
	// KÃ–PÃœK
	// ============================================================

	/** KÃ¶pÃ¼k yoÄŸunluÄŸu (varsayÄ±lan: 0.4). 0.0=kÃ¶pÃ¼k yok, 1.0=maksimum. */
	public float getFoamIntensity() { return foamIntensity; }
	public void setFoamIntensity(float foamIntensity) { this.foamIntensity = foamIntensity; }

	/** KÃ¶pÃ¼ÄŸÃ¼n gÃ¶rÃ¼ndÃ¼ÄŸÃ¼ dalga eÅŸiÄŸi (varsayÄ±lan: 1.2). */
	public float getFoamThreshold() { return foamThreshold; }
	public void setFoamThreshold(float foamThreshold) { this.foamThreshold = foamThreshold; }

	// ============================================================
	// POZÄ°SYON
	// ============================================================

	public float getX() { return x; }
	public void setX(float x) { this.x = x; }
	public float getZ() { return z; }
	public void setZ(float z) { this.z = z; }
	public float getHeight() { return height; }
	public void setHeight(float height) { this.height = height; }
	public float getSize() { return size; }
	public void setSize(float size) { this.size = size; }
	public float getTime() { return time; }
	public void setTime(float time) { this.time = time; }
	public float getTextureScale() { return textureScale; }
	public void setTextureScale(float scale) { this.textureScale = scale; }

	// ============================================================
	// SU ALTI FOG
	// ============================================================

	public float getWaterHeightAt(float wx, float wz) { return this.height; }
	public float getUnderwaterFogDensity() { return underwaterFogDensity; }
	public void setUnderwaterFogDensity(float d) { this.underwaterFogDensity = d; }
	public float getUnderwaterFogR() { return underwaterFogR; }
	public float getUnderwaterFogG() { return underwaterFogG; }
	public float getUnderwaterFogB() { return underwaterFogB; }
	public void setUnderwaterFogColor(float r, float g, float b) {
		this.underwaterFogR = r;
		this.underwaterFogG = g;
		this.underwaterFogB = b;
	}
}


