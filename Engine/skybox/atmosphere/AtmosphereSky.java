package skybox.atmosphere;

import skybox.ISky;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.lwjgl.util.vector.Vector3f;
import utils.ICamera;

/**
 * Prosedurel, shader tabanli atmosferik gokyuzu modeli.
 * Rengi gunesin konumuna ve atmosfer kalinligina gore dinamik hesaplanir.
 * Bulutlar fizik simulasyonu ile hareket eder.
 */
public class AtmosphereSky implements ISky {

	// Renderer referansi - MasterRenderer buraya inject eder
	private AtmosphereRenderer renderer;
	private boolean planetaryMode = false;

	// ==============================================================
	// BULUT FIZIK SISTEMI
	// ==============================================================
	/** Aktif bulut kumeleri */
	private List<CloudCluster> clusters = new ArrayList<>();
	/** Toplam gecen suresi (turbulans icin) */
	private float time = 0f;
	/** Ruzgar yonu (normalize) */
	private Vector3f windDir = new Vector3f(1f, 0f, 0.3f);
	/** Ruzgar hizi (birim/saniye) - Daha anlasilir olmasi icin artirildi */
	private float windSpeed = 350f;
	/** Kamera konumu (bulut spawn merkezi icin) */
	private Vector3f cameraRef = new Vector3f();
	/** Bulut havuzu boyutu */
	private int maxClusters = 40;
	/** Spawn alarti acikligi (saniye) */
	private float spawnTimer = 0f;
	private static final float SPAWN_INTERVAL = 4f;
	/** Bulutlarin kameranin etrafinda olusacagi kare yaricapi (5000x5000 alanÄ± kapsayacak ÅŸekilde) */
	private static final float SPAWN_RADIUS = 5000f;

	// Tekil, kalÄ±cÄ± Random Ã¼reteci (TÃ¼m bulutlarÄ±n aynÄ± noktada doÄŸmasÄ± hatasÄ±nÄ± giderir)
	private final Random rng = new Random();

	// Hareketli hava durumu cephe merkezi (BÃ¶lgesel bulutlanma iÃ§in)
	private final Vector3f weatherCellCenter = new Vector3f();
	private float weatherCellAngle = 0f;

	private float cloudScaleMultiplier = 2.5f;
	private float cloudMinHeight = 4000f;
	private float cloudMaxHeight = 4800f;
	private float windSpeedMultiplier = 1.0f;
	private float cloudDensityMultiplier = 1.0f;

	private environment.WeatherSystem weatherSystem;

	public AtmosphereSky() {
		// Baslangicta bulut yok, zamanla 0'dan baslayarak spawn edilecekler
	}

	public void setWeatherSystem(environment.WeatherSystem ws) {
		this.weatherSystem = ws;
	}

	public environment.WeatherSystem getWeatherSystem() {
		return this.weatherSystem;
	}

	// --- GELÄ°ÅžTÄ°RÄ°CÄ° API'SÄ° (CLOUD CONFIGURATION) ---
	public void setCloudScale(float scale) { this.cloudScaleMultiplier = scale; }
	public float getCloudScale() { return this.cloudScaleMultiplier; }

	public void setCloudHeightRange(float min, float max) { 
		this.cloudMinHeight = min; 
		this.cloudMaxHeight = max; 
	}
	public float getCloudMinHeight() { return cloudMinHeight; }
	public float getCloudMaxHeight() { return cloudMaxHeight; }

	public void setWindSpeedMultiplier(float mult) { this.windSpeedMultiplier = mult; }
	public float getWindSpeedMultiplier() { return windSpeedMultiplier; }

	public void setCloudDensityMultiplier(float mult) { this.cloudDensityMultiplier = mult; }
	public float getCloudDensityMultiplier() { return cloudDensityMultiplier; }
	// ------------------------------------------------

	/** Ruzgar yonunu ve hizini ayarla (Scene.getWindVelocity ile senkron tutulabilir) */
	public void setWind(Vector3f wind) {
		if (wind.lengthSquared() > 0.001f) {
			windDir.set(wind);
			windSpeed = windDir.length();
			windDir.normalise();
		} else {
			windSpeed = 0f;
		}
	}

	public void update(float delta, Vector3f camPos) {
		time += delta;
		cameraRef.set(camPos);

		// Dinamik Ruzgar Yonu: Zamanla yonu yavasca degistir
		float windAngle = time * 0.005f; // Cok yavas donus
		windDir.x = (float)Math.cos(windAngle);
		windDir.z = (float)Math.sin(windAngle * 0.7f);
		normWind();

		// BÃ¶lgesel hava durumu cephesini yavaÅŸÃ§a hareket ettir
		weatherCellAngle += delta * 0.02f;
		weatherCellCenter.x = camPos.x + (float) Math.cos(weatherCellAngle) * SPAWN_RADIUS * 0.4f;
		weatherCellCenter.z = camPos.z + (float) Math.sin(weatherCellAngle) * SPAWN_RADIUS * 0.4f;
		weatherCellCenter.y = 0;

		// Hava durumu dongusu guncellemesi
		float coverage;
		if (weatherSystem != null) {
			coverage = weatherSystem.getCurrentCoverage();
		} else {
			// Dinamik Bulut Yogunlugu: Bazen azalir bazen cok artar (Sinus dalgasi)
			// time/300.0f yaklasik 5 dakikalik bir dongu yaratir
			coverage = 0.5f + (float)Math.sin(time / 150.0f) * 0.4f; // 0.1 ile 0.9 arasi
			coverage = Math.max(0.0f, Math.min(1.0f, coverage));
		}
		this.maxClusters = (int)(coverage * 150f);
		this.cloudDensityMultiplier = (0.5f + coverage * 1.0f) * (weatherSystem != null ? weatherSystem.getCloudDensityMultiplier() : 1.0f);
		this.cloudScaleMultiplier = 2.5f + coverage * 5.5f;

		// Kamera uzaklasmis bulutlari oldurmek icin:
		if (!planetaryMode) {
			for (CloudCluster c : clusters) {
				float dx = c.position.x - camPos.x;
				float dz = c.position.z - camPos.z;
				if ((float)Math.sqrt(dx*dx + dz*dz) > SPAWN_RADIUS * 1.5f) {
					c.dying = true;
				}
			}
		}

		// Listeden bitmisleri temizle
		clusters.removeIf(c -> c.isDead());

		// Hava durumu temizlesmeye gectiginde fazla bulutlari yavasca sondur
		if (clusters.size() > maxClusters) {
			for (int i = maxClusters; i < clusters.size(); i++) {
				clusters.get(i).dying = true;
			}
		}

		// Eksik bulutlari tamamla
		boolean isInitial = clusters.isEmpty();
		while (clusters.size() < maxClusters) {
			spawnCluster(isInitial);
		}

		// BulutlarÄ± fizik olarak hareket ettir (Merging / Birlestirme tamamen kaldirildi)

		// BulutlarÄ± gÃ¼ncelle (Ã§ift hareket gÃ¼ncellemesi hatasÄ± giderildi, CloudCluster gÃ¼ncelliyor)
		for (CloudCluster c : clusters) {
			int nearby = 0;
			for (CloudCluster other : clusters) {
				if (c == other || c.layer != other.layer) continue;
				float dx = c.position.x - other.position.x;
				float dz = c.position.z - other.position.z;
				float dist = (float)Math.sqrt(dx*dx + dz*dz);
				if (dist < 600f) nearby++;
			}

			if (planetaryMode) {
				float dist = (float)Math.sqrt(c.position.x * c.position.x + c.position.z * c.position.z);
				float angle = (float)Math.atan2(c.position.z, c.position.x);
				angle += windSpeed * windSpeedMultiplier * delta * 0.005f;
				c.position.x = (float)Math.cos(angle) * dist;
				c.position.z = (float)Math.sin(angle) * dist;
				c.update(delta, windDir, 0f, time, nearby); // Planetary modda rÃ¼zgar Ã¶telemesi yok
			} else {
				c.update(delta, windDir, windSpeed * windSpeedMultiplier, time, nearby);
			}
			c.density = c.density * cloudDensityMultiplier;
		}
	}

	private void spawnCluster(boolean fastForward) {
		long seed = rng.nextLong();
		int layer = rng.nextInt(3); // 3 katman (0, 1, 2)
		float minH = (weatherSystem != null) ? weatherSystem.getLayerMinHeight(layer) : cloudMinHeight;
		float maxH = (weatherSystem != null) ? weatherSystem.getLayerMaxHeight(layer) : cloudMaxHeight;
		float y = minH + rng.nextFloat() * (maxH - minH);

		float x = 0, z = 0;
		float coverage = (weatherSystem != null) ? weatherSystem.getCurrentCoverage() : 0.5f;

		if (planetaryMode) {
			float dist = 110f + rng.nextFloat() * 30f;
			float heightAngle = (rng.nextFloat() - 0.5f) * (float)Math.PI;
			float r = dist * (float)Math.cos(heightAngle);
			y = dist * (float)Math.sin(heightAngle);
			float angle = rng.nextFloat() * 2f * (float)Math.PI;
			x = r * (float)Math.cos(angle);
			z = r * (float)Math.sin(angle);
		} else {
			// Organik bulut kÃ¼meleri iÃ§in gÃ¼rÃ¼ltÃ¼ (noise) tabanlÄ± kabul/ret Ã¶rneklemesi
			boolean accepted = false;
			int attempts = 0;
			while (!accepted && attempts < 30) {
				attempts++;
				float angle;
				float dist;
				
				if (fastForward) {
					// Ä°lk dolum: KameranÄ±n etrafÄ±nda 5000x5000 geniÅŸ alana tamamen homojen daÄŸÄ±t
					angle = rng.nextFloat() * 2f * (float)Math.PI;
					dist = (float)Math.sqrt(rng.nextFloat()) * SPAWN_RADIUS;
					x = cameraRef.x + (float)Math.cos(angle) * dist;
					z = cameraRef.z + (float)Math.sin(angle) * dist;
				} else {
					// Ã‡alÄ±ÅŸma zamanÄ±: RÃ¼zgar yÃ¶nÃ¼nÃ¼n tersindeki ufuk Ã§izgisinden sÃ¼zÃ¼lerek girsinler
					float windAngle = (float) Math.atan2(windDir.z, windDir.x);
					// RÃ¼zgar yÃ¶nÃ¼nÃ¼n tersi (+PI) ve genis sapma payÄ±
					angle = windAngle + (float) Math.PI + (rng.nextFloat() - 0.5f) * ((float) Math.PI * 0.8f);
					dist = SPAWN_RADIUS;
					x = cameraRef.x + (float)Math.cos(angle) * dist;
					z = cameraRef.z + (float)Math.sin(angle) * dist;
				}

				// Hava durumu yoÄŸunluÄŸuna gÃ¶re gÃ¼rÃ¼ltÃ¼ fonksiyonunu sorgula
				float noiseVal = getNoise(x, z, time);
				float threshold = 1.0f - coverage;
				if (noiseVal >= threshold || attempts == 30) {
					accepted = true;
				}
			}
		}

		Vector3f wDir = new Vector3f(windDir);
		if (wDir.lengthSquared() < 0.0001f) wDir.set(1f, 0f, 0f);

		CloudCluster cluster = new CloudCluster(x, y, z, wDir, windSpeed, seed, cloudScaleMultiplier, layer);
		
		if (fastForward) {
			cluster.setAge(15f + rng.nextFloat() * 20f);
			cluster.alpha = 1.0f; 
		} else {
			cluster.setAge(0f);
			cluster.alpha = 0.0f; // Zamanla fade-in olacak
		}
		
		clusters.add(cluster);
	}

	private float getNoise(float x, float z, float time) {
		// Organik hava cepheleri iÃ§in gÃ¼rÃ¼ltÃ¼ frekanslarÄ± (daha sÄ±kÄ± ve parÃ§alÄ± bulutlar iÃ§in)
		// Wavelength = ~2000-4000 birim (5000'lik haritada birden fazla kÃ¼me oluÅŸmasÄ±nÄ± saÄŸlar)
		float s1 = 0.0015f;
		float s2 = 0.003f;
		float s3 = 0.006f;
		
		// GÃ¼rÃ¼ltÃ¼ haritasÄ±nÄ± rÃ¼zgar yÃ¶nÃ¼ ile hareket ettir ki bulutlar gÃ¼rÃ¼ltÃ¼ formasyonunun iÃ§inde kalsÄ±n
		float driftX = -time * windDir.x * windSpeed * windSpeedMultiplier;
		float driftZ = -time * windDir.z * windSpeed * windSpeedMultiplier;
		
		double nx = x + driftX;
		double nz = z + driftZ;
		
		// Hava durumunun kendi iÃ§ evrimi (bulutlarÄ±n yavaÅŸÃ§a ÅŸekil deÄŸiÅŸtirmesi)
		double evolution = time * 0.02;
		
		double val = Math.sin(nx * s1 + evolution) * Math.cos(nz * s1 - evolution)
				   + Math.sin(nx * s2) * Math.sin(nz * s2) * 0.6
				   + Math.cos(nx * s3) * Math.cos(nz * s3) * 0.3;
		
		// [-1.9, 1.9] aralÄ±ÄŸÄ±ndan [0, 1] arasÄ±na normalize et
		return (float) ((val / 1.9) * 0.5 + 0.5);
	}

	private void normWind() {
		if (windDir.lengthSquared() > 0.0001f) windDir.normalise();
	}

	public List<CloudCluster> getClusters() { return clusters; }
	public float getTime()                  { return time; }


	/** ISky arayuzu: MasterRenderer tarafindan polymorphic olarak cagirilir. */
	@Override
	public void render(ICamera camera, Vector3f lightDir) {
		if (renderer != null) {
			renderer.render(this, camera, lightDir);
		}
	}

	/** MasterRenderer bu atmosferin renderer'ini inject eder */
	public void setRenderer(AtmosphereRenderer r) { this.renderer = r; }

	// Atmosferin temel ozellikleri
	private Vector3f sunPosition = new Vector3f(0, 100, 0); 
	private float atmosphereThickness = 1.0f;
	private Vector3f skyColorDay = new Vector3f(0.5f, 0.7f, 1.0f);
	private Vector3f skyColorSunset = new Vector3f(1.0f, 0.4f, 0.1f);
	private Vector3f spaceColor = new Vector3f(0.01f, 0.01f, 0.02f);
	private boolean cloudsEnabled = true;

	// --- Getters / Setters ---
	public Vector3f getSunPosition()       { return sunPosition; }
	public float getAtmosphereThickness()  { return atmosphereThickness; }
	public Vector3f getSkyColorDay()       { return skyColorDay; }
	public Vector3f getSkyColorSunset()    { return skyColorSunset; }
	public Vector3f getSpaceColor()        { return spaceColor; }
	public boolean isCloudsEnabled()       { return cloudsEnabled; }

	public void setSunPosition(Vector3f v)      { sunPosition = v; }
	public void setAtmosphereThickness(float t) { atmosphereThickness = t; }
	public void setSkyColorDay(Vector3f v)      { skyColorDay = v; }
	public void setSkyColorSunset(Vector3f v)   { skyColorSunset = v; }
	public void setSpaceColor(Vector3f v)       { spaceColor = v; }
	public void setCloudsEnabled(boolean b)     { cloudsEnabled = b; }
	public void setPlanetaryMode(boolean b)     { planetaryMode = b; }
	public boolean isPlanetaryMode()            { return planetaryMode; }

	private float fogDensity = 0.0f;
	public float getFogDensity() { return fogDensity; }
	public void setFogDensity(float d) { this.fogDensity = d; }

	private Vector3f fogColor = new Vector3f(1, 1, 1);
	public Vector3f getFogColor() { return fogColor; }
	public void setFogColor(Vector3f c) { this.fogColor = c; }

	@Override
	public void cleanUp() {
		// Atmosfer modeli sadece matematiksel
	}
}

