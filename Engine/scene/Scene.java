package scene;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;

import skybox.classic.Skybox;
import terrain.ITerrain;
import textures.Texture;
import utils.ICamera;
import water.tile.WaterTile;

/**
 * Oyun iÃƒÂ§i sahne yapÃ„Â±sÃ„Â±nÃ„Â± barÃ„Â±ndÃ„Â±ran merkezi sÃ„Â±nÃ„Â±f.
 * KamerayÃ„Â±, gÃƒÂ¶kyÃƒÂ¼zÃƒÂ¼nÃƒÂ¼, Ã„Â±Ã…Å¸Ã„Â±k yÃƒÂ¶nÃƒÂ¼nÃƒÂ¼, su yÃƒÂ¼zeylerini ve sahnedeki tÃƒÂ¼m nesneleri
 * render iÃ…Å¸lemlerinde kolaylÃ„Â±k saÃ„Å¸lamak iÃƒÂ§in kategorize edilmiÃ…Å¸ listelerde (yansÃ„Â±yan, suda gÃƒÂ¶rÃƒÂ¼nen vb.) tutar.
 */
public class Scene {

	// Standart olarak ÃƒÂ§izilecek tÃƒÂ¼m temel nesneler
	private List<Entity> standardEntities = new ArrayList<Entity>();
	// Batching kapalÄ± olduÄŸunda her karede Ã§izilecek olan bireysel aÄŸaÃ§ ve Ã§imen listesi
	private List<Entity> unbatchedFlora = new ArrayList<Entity>();
	// Su yÃƒÂ¼zeyinde yansÃ„Â±masÃ„Â± gÃƒÂ¶rÃƒÂ¼lecek nesneler
	private List<Entity> reflectableEntities = new ArrayList<Entity>();
	// Su altÃ„Â±ndayken kÃ„Â±rÃ„Â±larak gÃƒÂ¶rÃƒÂ¼lecek nesneler
	private List<Entity> underwaterEntities = new ArrayList<Entity>();
	// DÃƒÂ¼Ã…Å¸ÃƒÂ¼k kalite / yansÃ„Â±ma haritasÃ„Â± ÃƒÂ§izimlerinde dahi renderlanmasÃ„Â± istenen ÃƒÂ¶nemli nesneler (Ãƒâ€“rn: DaÃ„Å¸, Zemin)
	private List<Entity> importantEntities = new ArrayList<Entity>();
	// ÃƒÅ“zerinde ÃƒÂ§evresel yansÃ„Â±ma (Environment Map) bulunduran parlak nesneler
	private List<Entity> shinyEntities = new ArrayList<Entity>();
	
	// Sahnedeki su yÃƒÂ¼zeylerini (kare Ã…Å¸eklindeki parÃƒÂ§alarÃ„Â±) tutan liste
	private List<WaterTile> waterTiles = new ArrayList<WaterTile>();
	
	// Terrain (Arazi) sistemi Ã¢â‚¬â€ ITerrain listesi
	private List<ITerrain> terrains = new ArrayList<ITerrain>();
	private terrain.GrassField grassField;
	
	// Instanced Rendering (Ãƒâ€¡oklu Ãƒâ€“rnekleme) verileri (Grass3D, Tree3D vb. iÃƒÂ§in)
	private java.util.Map<scene.Model, java.util.Map<scene.Skin, List<scene.InstanceData>>> instancedEntities = new java.util.HashMap<>();

	// Ãƒâ€¡oklu Nokta IÃ…Å¸Ã„Â±klarÃ„Â± (Multiple Point Lights)
	private List<Light> pointLights = new ArrayList<>();
	
	// Sahneyi ÃƒÂ§ekecek ana kamera
	private ICamera camera;
	// GÃƒÂ¼neÃ…Å¸ / Ana Ã„Â±Ã…Å¸Ã„Â±k kaynaÃ„Å¸Ã„Â±nÃ„Â±n sahneye geliÃ…Å¸ yÃƒÂ¶nÃƒÂ¼ (VarsayÃ„Â±lan olarak direkt yukarÃ„Â±dan aÃ…Å¸aÃ„Å¸Ã„Â±ya (0, -1, 0))
	private Vector3f lightDirection = new Vector3f(0, -1, 0);
	
	// GÃƒÂ¼neÃ…Å¸ IÃ…Å¸Ã„Â±Ã„Å¸Ã„Â±nÃ„Â±n Rengi (VarsayÃ„Â±lan: Beyaz)
	private Vector3f lightColor = new Vector3f(1.0f, 1.0f, 1.0f);
	// GÃƒÂ¼neÃ…Å¸ IÃ…Å¸Ã„Â±Ã„Å¸Ã„Â±nÃ„Â±n GÃƒÂ¼cÃƒÂ¼ / ParlaklÃ„Â±Ã„Å¸Ã„Â±
	private float lightBrightness = 0.8f;
	// Ortam IÃ…Å¸Ã„Â±Ã„Å¸Ã„Â± (GÃƒÂ¶lgede kalan, gÃƒÂ¼neÃ…Å¸ gÃƒÂ¶rmeyen yerlerin minimum parlaklÃ„Â±Ã„Å¸Ã„Â±)
	private float ambientLight = 0.5f;
	
	// Nokta IÃ…Å¸Ã„Â±Ã„Å¸Ã„Â± (Belirli bir noktadan yayÃ„Â±lan Ã„Â±Ã…Å¸Ã„Â±k)
	private Light pointLight;
	
	// Güneş (Dokulu)
	private sunRenderer.Sun sun;
	
	// Arka planÃ„Â± sÃƒÂ¼sleyecek gÃƒÂ¶kyÃƒÂ¼zÃƒÂ¼ arayÃƒÂ¼zÃƒÂ¼ (FotoÃ„Å¸raf veya Atmosfer olabilir)
	private skybox.ISky sky;
	
	// Dinamik kÃƒÂ¼p haritasÃ„Â± (Shiny nesnelerin ÃƒÂ§evre yansÃ„Â±malarÃ„Â± iÃƒÂ§in anlÃ„Â±k oluÃ…Å¸turulur)
	private Texture environmentMap;
	
	// VarsayÃ„Â±lan su yÃƒÂ¼ksekliÃ„Å¸i (-0.1f) - genelde dÃ„Â±Ã…Å¸arÃ„Â±dan yÃƒÂ¼klenir/deÃ„Å¸iÃ…Å¸tirilir
	private float waterHeight = -0.1f;
	// Sahnede su render edilsin mi bayraÃ„Å¸Ã„Â±
	private boolean renderWater = true;
	
	// Global ruzgar vektoru
	private Vector3f windVelocity = new Vector3f(5.0f, 0.0f, 2.0f);

	// Atmosferik sis (Uzaklik fogu Ã¢â‚¬â€ terrain, ocean vs icin)
	private Vector3f fogColor   = new Vector3f(0.72f, 0.82f, 0.92f); // Hafif mavi-gri sis
	private float fogDensity    = 0.80f;  // 0=sis yok, 1=maksimum sis
	private float fogStart      = 1800f;  // Kameradan kac birim sonra sis baslar
	
	// Dinamik Hava Durumu Sistemi
	private environment.WeatherSystem weatherSystem;

	// Frustum Culling aktif mi?
	private boolean frustumCullingEnabled = true;
	
	// Level of Detail (LOD) aktif mi?
	private boolean lodEnabled = true;

	public boolean isLodEnabled() {
		return lodEnabled;
	}

	public void setLodEnabled(boolean lodEnabled) {
		this.lodEnabled = lodEnabled;
	}

	// Occlusion Culling aktif mi?
	private boolean occlusionCullingEnabled = false;

	public Scene(ICamera camera) {
		this.camera = camera;
	}

	public boolean isFrustumCullingEnabled() {
		return frustumCullingEnabled;
	}

	public void setFrustumCullingEnabled(boolean frustumCullingEnabled) {
		this.frustumCullingEnabled = frustumCullingEnabled;
	}

	public boolean isOcclusionCullingEnabled() {
		return occlusionCullingEnabled;
	}

	public void setOcclusionCullingEnabled(boolean occlusionCullingEnabled) {
		this.occlusionCullingEnabled = occlusionCullingEnabled;
	}

	/**
	 * Yeni bir sahne oluÃ…Å¸turur (Su ve GÃƒÂ¶kyÃƒÂ¼zÃƒÂ¼ varsayÃ„Â±lan olarak aÃƒÂ§Ã„Â±ktÃ„Â±r).
	 * 
	 * @param camera Oyuncu kamerasÃ„Â±
	 * @param sky GÃƒÂ¶kyÃƒÂ¼zÃƒÂ¼ kaplamasÃ„Â± (null ise default yÃƒÂ¼klenir)
	 */
	public Scene(ICamera camera, Skybox sky) {
		this(camera, sky, true, true);
	}

	/**
	 * Su render edilip edilmeyeceÃ„Å¸i belirtilerek sahne oluÃ…Å¸turur.
	 * 
	 * @param camera Oyuncu kamerasÃ„Â±
	 * @param sky GÃƒÂ¶kyÃƒÂ¼zÃƒÂ¼ kaplamasÃ„Â±
	 * @param renderWater Su render edilecekse true
	 */
	public Scene(ICamera camera, skybox.ISky sky, boolean renderWater) {
		this(camera, sky, true, renderWater);
	}
	
	/**
	 * Su ve gÃƒÂ¶kyÃƒÂ¼zÃƒÂ¼ render ayarlarÃ„Â± belirlenerek sahne oluÃ…Å¸turur.
	 * 
	 * @param camera Oyuncu kamerasÃ„Â±
	 * @param sky GÃƒÂ¶kyÃƒÂ¼zÃƒÂ¼ kaplamasÃ„Â± (null ise ve renderSky true ise varsayÃ„Â±lan yÃƒÂ¼klenir)
	 * @param renderSky GÃƒÂ¶kyÃƒÂ¼zÃƒÂ¼ render edilecekse true
	 * @param renderWater Su render edilecekse true
	 */
	public Scene(ICamera camera, skybox.ISky sky, boolean renderSky, boolean renderWater) {
		this.camera = camera;
		
		if (sky == null && renderSky) {
			this.sky = null;
		} else {
			this.sky = sky;
		}
		
		this.renderWater = renderWater;
		
		environmentMap = Texture.newEmptyCubeMap(256);
		
		this.weatherSystem = new environment.WeatherSystem(this);
		if (this.sky != null && this.sky instanceof skybox.atmosphere.AtmosphereSky) {
			((skybox.atmosphere.AtmosphereSky) this.sky).setWeatherSystem(this.weatherSystem);
		}
	}

	/** Ana Ã„Â±Ã…Å¸Ã„Â±Ã„Å¸Ã„Â±n (gÃƒÂ¼neÃ…Å¸in) sahneye geliÃ…Å¸ yÃƒÂ¶nÃƒÂ¼nÃƒÂ¼ ayarlar ve vektÃƒÂ¶rÃƒÂ¼ normalleÃ…Å¸tirir. */
	public void setLightDirection(Vector3f direction) {
		direction.normalise();
		this.lightDirection.set(direction);
	}

	/**
	 * Su yÃƒÂ¼zeylerindeki obje etkileÃ…Å¸imlerini gÃƒÂ¼nceller.
	 */
	public void updateWaterInteractions(float delta) {
		// No longer used in Asylum Water
	}
	
	/** @return Dinamik olarak hesaplanmÃ„Â±Ã…Å¸ ÃƒÂ§evresel kÃƒÂ¼p haritasÃ„Â± dokusunu dÃƒÂ¶ndÃƒÂ¼rÃƒÂ¼r */
	public Texture getEnvironmentMap(){
		return environmentMap;
	}
	
	/** @return Su seviyesinin Y eksenindeki konumunu dÃƒÂ¶ndÃƒÂ¼rÃƒÂ¼r */
	public float getWaterHeight(){
		return waterHeight;
	}

	/** @return Sahnedeki su bloklarÃ„Â±nÃ„Â±n listesini dÃƒÂ¶ndÃƒÂ¼rÃƒÂ¼r (KapalÃ„Â±ysa boÃ…Å¸ liste) */
	public List<WaterTile> getWater() {
		return renderWater ? waterTiles : new ArrayList<WaterTile>();
	}

	/**
	 * Engine tarafÃ„Â±ndan varsayÃ„Â±lan olarak eklenen ilk su yÃƒÂ¼zeyini dÃƒÂ¶ndÃƒÂ¼rÃƒÂ¼r.
	 * @return VarsayÃ„Â±lan WaterTile, yoksa null
	 */
	public WaterTile getDefaultWater() {
		if (waterTiles.isEmpty()) return null;
		return waterTiles.get(0);
	}

	/**
	 * Yeni terrain sistemine ITerrain ekler (FlatTerrain, VoxelTerrain vb.).
	 */
	public void addTerrain(ITerrain terrain) {
		terrains.add(terrain);
	}

	public void addInstancedEntity(scene.Model model, scene.Skin skin, org.lwjgl.util.vector.Matrix4f transform, float textureOffsetIndex) {
		instancedEntities.putIfAbsent(model, new java.util.HashMap<>());
		java.util.Map<scene.Skin, List<scene.InstanceData>> skinMap = instancedEntities.get(model);
		skinMap.putIfAbsent(skin, new ArrayList<>());
		skinMap.get(skin).add(new scene.InstanceData(transform, textureOffsetIndex));
	}
	
	public java.util.Map<scene.Model, java.util.Map<scene.Skin, List<scene.InstanceData>>> getInstancedEntities() {
		return instancedEntities;
	}

	/**
	 * SahneyÃÂµ bir su yÃƒÂ¼zeyi (WaterTile) ekler.
	 * @param tile Eklenecek su parcasi
	 */
	public void addWater(water.tile.WaterTile tile) {
		waterTiles.add(tile);
		this.waterHeight = tile.getHeight();
	}

	/** @return Sahnedeki tÃƒÂ¼m terrain'lerin listesi */
	public List<ITerrain> getTerrains() {
		return terrains;
	}

	/**
	 * Eski Entity-tabanlÃ„Â± terrain ekleme (Geriye uyumluluk iÃƒÂ§in korundu).
	 * Yeni kodda addTerrain(ITerrain) kullanÃ„Â±n.
	 */
	public void addTerrainEntity(Entity terrain) {
		standardEntities.add(terrain);
		importantEntities.add(terrain);
		reflectableEntities.add(terrain);
		underwaterEntities.add(terrain);
	}
	
	/**
	 * Parlak, yansÃ„Â±ma yapan ÃƒÂ¶zel nesneleri (Metal top vb.) ilgili listelere ekler.
	 */
	public void addShiny(Entity entity){
		if(entity.isSeenUnderWater()){
			underwaterEntities.add(entity);
		}
		if(entity.hasReflection()){
			reflectableEntities.add(entity);
		}
		shinyEntities.add(entity);
	}

	/**
	 * Sahneye standart bir nesne (Ağaç, Sandık vb.) ekler ve özelliklerine göre doğru alt listelere dağıtır.
	 */
	public void addEntity(Entity entity) {
		standardEntities.add(entity);
		if(entity.isSeenUnderWater()){
			underwaterEntities.add(entity);
		}
		if(entity.hasReflection()){
			reflectableEntities.add(entity);
		}
		if(entity.isImportant()){
			importantEntities.add(entity);
		}
		
		// Eğer bu obje çoklu parçadan oluşuyorsa (Multi-Mesh) parçaları da otomatik sahneye ekle
		if (entity instanceof GameObject) {
			GameObject go = (GameObject) entity;
			if (go.getMultiMeshParts() != null) {
				for (GameObject part : go.getMultiMeshParts()) {
					addEntity(part);
				}
			}
		}
	}

	public void removeEntity(Entity entity) {
		standardEntities.remove(entity);
		underwaterEntities.remove(entity);
		reflectableEntities.remove(entity);
		importantEntities.remove(entity);
		shinyEntities.remove(entity);
		
		if (entity instanceof GameObject) {
			GameObject go = (GameObject) entity;
			if (go.getMultiMeshParts() != null) {
				for (GameObject part : go.getMultiMeshParts()) {
					removeEntity(part);
				}
			}
		}
	}

	/** @return Sahnenin gÃƒÂ¶kyÃƒÂ¼zÃƒÂ¼ objesini dÃƒÂ¶ndÃƒÂ¼rÃƒÂ¼r */
	public skybox.ISky getSky() {
		return sky;
	}

	public void setSky(skybox.ISky sky) {
		this.sky = sky;
		// AtmosphereSky ise renderer'ini inject et (once MasterRenderer'a gerek kalmaz)
		if (sky instanceof skybox.atmosphere.AtmosphereSky) {
			skybox.atmosphere.AtmosphereSky atmo = (skybox.atmosphere.AtmosphereSky) sky;
			atmo.setRenderer(new skybox.atmosphere.AtmosphereRenderer());
			atmo.setWeatherSystem(this.weatherSystem);
		}
	}

	/** @return Ana Ã„Â±Ã…Å¸Ã„Â±k (gÃƒÂ¼neÃ…Å¸) yÃƒÂ¶n vektÃƒÂ¶rÃƒÂ¼nÃƒÂ¼ dÃƒÂ¶ndÃƒÂ¼rÃƒÂ¼r */
	public Vector3f getLightDirection() {
		return lightDirection;
	}

	public sunRenderer.Sun getSun() {
		return sun;
	}

	public void setSun(sunRenderer.Sun sun) {
		this.sun = sun;
	}

	public Vector3f getLightColor() {
		return lightColor;
	}

	public void setLightColor(Vector3f lightColor) {
		this.lightColor = lightColor;
	}

	public float getLightBrightness() {
		return lightBrightness;
	}

	public void setLightBrightness(float lightBrightness) {
		this.lightBrightness = lightBrightness;
	}

	public float getAmbientLight() {
		return ambientLight;
	}

	public void setAmbientLight(float ambientLight) {
		this.ambientLight = ambientLight;
	}

	public List<Light> getPointLights() {
		return pointLights;
	}

	public void addPointLight(Light light) {
		pointLights.add(light);
	}
	
	public void clearPointLights() {
		pointLights.clear();
	}

	public Light getPointLight() {
		return pointLight;
	}

	public void setPointLight(Light pointLight) {
		this.pointLight = pointLight;
	}

	/** @return Sahnenin bakÃ„Â±Ã…Å¸ aÃƒÂ§Ã„Â±sÃ„Â±nÃ„Â± saÃ„Å¸layan kamerayÃ„Â± dÃƒÂ¶ndÃƒÂ¼rÃƒÂ¼r */
	public ICamera getCamera() {
		return camera;
	}

	public void setCamera(ICamera camera) {
		this.camera = camera;
	}
	
	/** @return Su yÃƒÂ¼zeyinde yansÃ„Â±masÃ„Â± ÃƒÂ§izilmesi gereken objelerin listesi */
	public List<Entity> getReflectedEntities() {
		return reflectableEntities;
	}
	
	/** @return DÃƒÂ¼Ã…Å¸ÃƒÂ¼k kalite yansÃ„Â±malarda bile ÃƒÂ§izilmesi gereken ana objelerin listesi */
	public List<Entity> getImportantEntities() {
		return importantEntities;
	}
	
	/** @return ÃƒÅ“zerine ÃƒÂ§evre yansÃ„Â±masÃ„Â± uygulanacak parlak nesnelerin listesi */
	public List<Entity> getShinyEntities() {
		return shinyEntities;
	}
	
	/** @return Suyun altÃ„Â±ndayken (kÃ„Â±rÃ„Â±lma etkisiyle) gÃƒÂ¶rÃƒÂ¼nen objelerin listesi */
	public List<Entity> getUnderwaterEntities() {
		return underwaterEntities;
	}

	/** @return Sahnedeki standart tÃƒÂ¼m nesnelerin listesi */
	public List<Entity> getAllEntities() {
		return standardEntities;
	}

	public List<Entity> getUnbatchedFlora() {
		return unbatchedFlora;
	}

	public void clearUnbatchedFlora() {
		unbatchedFlora.clear();
	}

	/**
	 * Sahne kapatÃ„Â±lÃ„Â±rken gÃƒÂ¶kyÃƒÂ¼zÃƒÂ¼nÃƒÂ¼, tÃƒÂ¼m nesneleri ve yansÃ„Â±ma haritasÃ„Â±nÃ„Â± GPU belleÃ„Å¸inden tamamen temizler.
	 */
	public void delete() {
		if (sky != null) {
			sky.cleanUp();
		}
		for (Entity entity : standardEntities) {
			entity.delete();
		}
		for (ITerrain t : terrains) {
			t.cleanUp();
		}
		if (grassField != null) {
			grassField.cleanUp();
		}
		if (environmentMap != null) {
			environmentMap.delete();
		}
	}

	public terrain.GrassField getGrassField() { return grassField; }
	public void setGrassField(terrain.GrassField grass) { this.grassField = grass; }

	public Vector3f getWindVelocity() { return windVelocity; }
	public void setWindVelocity(Vector3f windVelocity) { this.windVelocity.set(windVelocity); }

	// Fog API
	public Vector3f getFogColor()    { return fogColor; }
	public float    getFogDensity()  { return fogDensity; }
	public float    getFogStart()    { return fogStart; }
	public void setFogColor(Vector3f c)  { this.fogColor.set(c); }
	public void setFogDensity(float d)   { this.fogDensity = d; }
	public void setFogStart(float s)     { this.fogStart = s; }

	public environment.WeatherSystem getWeatherSystem() {
		return weatherSystem;
	}

	public void setWeatherSystem(environment.WeatherSystem weatherSystem) {
		this.weatherSystem = weatherSystem;
		if (sky != null && sky instanceof skybox.atmosphere.AtmosphereSky) {
			((skybox.atmosphere.AtmosphereSky) sky).setWeatherSystem(weatherSystem);
		}
	}
}


