package scene;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;

import skybox.Skybox;
import textures.Texture;
import utils.ICamera;
import water.WaterTile;

/**
 * Oyun içi sahne yapısını barındıran merkezi sınıf.
 * Kamerayı, gökyüzünü, ışık yönünü, su yüzeylerini ve sahnedeki tüm nesneleri
 * render işlemlerinde kolaylık sağlamak için kategorize edilmiş listelerde (yansıyan, suda görünen vb.) tutar.
 */
public class Scene {

	// Objeleri farklı listelerde birden fazla kez saklamak büyük sahnelerde verimsiz olabilir,
	// ancak sadece birkaç objenin olduğu bu basit mimari için uygundur (Performans sorununa yol açmaz).
	
	// Standart olarak çizilecek tüm temel nesneler
	private List<Entity> standardEntities = new ArrayList<Entity>();
	// Su yüzeyinde yansıması görülecek nesneler
	private List<Entity> reflectableEntities = new ArrayList<Entity>();
	// Su altındayken kırılarak görülecek nesneler
	private List<Entity> underwaterEntities = new ArrayList<Entity>();
	// Düşük kalite / yansıma haritası çizimlerinde dahi renderlanması istenen önemli nesneler (Örn: Dağ, Zemin)
	private List<Entity> importantEntities = new ArrayList<Entity>();
	// Üzerinde çevresel yansıma (Environment Map) bulunduran parlak nesneler
	private List<Entity> shinyEntities = new ArrayList<Entity>();
	
	// Sahnedeki su yüzeylerini (kare şeklindeki parçaları) tutan liste
	private List<WaterTile> waterTiles = new ArrayList<WaterTile>();
	
	// Sahneyi çekecek ana kamera
	private ICamera camera;
	// Güneş / Ana ışık kaynağının sahneye geliş yönü (Varsayılan olarak direkt yukarıdan aşağıya (0, -1, 0))
	private Vector3f lightDirection = new Vector3f(0, -1, 0);
	// Arka planı süsleyecek gökyüzü küpü
	private Skybox sky;
	
	// Dinamik küp haritası (Shiny nesnelerin çevre yansımaları için anlık oluşturulur)
	private Texture environmentMap;
	
	// Varsayılan su yüksekliği (-0.1f) - genelde dışarıdan yüklenir/değiştirilir
	private float waterHeight = -0.1f;
	// Sahnede su render edilsin mi bayrağı
	private boolean renderWater = true;

	/**
	 * Yeni bir sahne oluşturur (Su varsayılan olarak açıktır).
	 * 
	 * @param camera Oyuncu kamerası
	 * @param sky Gökyüzü kaplaması
	 */
	public Scene(ICamera camera, Skybox sky) {
		this(camera, sky, true);
	}

	/**
	 * Su render edilip edilmeyeceği belirtilerek sahne oluşturur.
	 * 
	 * @param camera Oyuncu kamerası
	 * @param sky Gökyüzü kaplaması
	 * @param renderWater Su render edilecekse true
	 */
	public Scene(ICamera camera, Skybox sky, boolean renderWater) {
		this.camera = camera;
		this.sky = sky;
		this.renderWater = renderWater;
		
		// 256x256 çözünürlüğünde boş bir küp haritası yaratır (Parlak objeler bu haritayı kullanır)
		environmentMap = Texture.newEmptyCubeMap(256);
		
		// Eğer su çizilecekse örnek su parçacıkları ekle
		if (renderWater) {
			waterTiles.add(new WaterTile(-20, 6, waterHeight));
			waterTiles.add(new WaterTile(-10, 6, waterHeight));
			waterTiles.add(new WaterTile(0, 6, waterHeight));
			waterTiles.add(new WaterTile(10, 6, waterHeight));
		}
	}

	/** Ana ışığın (güneşin) sahneye geliş yönünü ayarlar ve vektörü normalleştirir. */
	public void setLightDirection(Vector3f direction) {
		direction.normalise();
		this.lightDirection.set(direction);
	}
	
	/** @return Dinamik olarak hesaplanmış çevresel küp haritası dokusunu döndürür */
	public Texture getEnvironmentMap(){
		return environmentMap;
	}
	
	/** @return Su seviyesinin Y eksenindeki konumunu döndürür */
	public float getWaterHeight(){
		return waterHeight;
	}

	/** @return Sahnedeki su bloklarının listesini döndürür (Kapalıysa boş liste) */
	public List<WaterTile> getWater() {
		return renderWater ? waterTiles : new ArrayList<WaterTile>();
	}

	/**
	 * Sahneye zemin/yeryüzü olarak hizmet edecek nesneyi ekler.
	 * Arazi (Terrain) olduğu için hem standart, hem önemli, hem yansıyan hem de suda görünen listelerine eklenir.
	 */
	public void addTerrain(Entity terrain) {
		standardEntities.add(terrain);
		importantEntities.add(terrain);
		reflectableEntities.add(terrain);
		underwaterEntities.add(terrain);
	}
	
	/**
	 * Parlak, yansıma yapan özel nesneleri (Metal top vb.) ilgili listelere ekler.
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
	}

	/** @return Sahnenin gökyüzü objesini döndürür */
	public Skybox getSky() {
		return sky;
	}

	/** @return Ana ışık (güneş) yön vektörünü döndürür */
	public Vector3f getLightDirection() {
		return lightDirection;
	}

	/** @return Sahnenin bakış açısını sağlayan kamerayı döndürür */
	public ICamera getCamera() {
		return camera;
	}
	
	/** @return Su yüzeyinde yansıması çizilmesi gereken objelerin listesi */
	public List<Entity> getReflectedEntities() {
		return reflectableEntities;
	}
	
	/** @return Düşük kalite yansımalarda bile çizilmesi gereken ana objelerin listesi */
	public List<Entity> getImportantEntities() {
		return importantEntities;
	}
	
	/** @return Üzerine çevre yansıması uygulanacak parlak nesnelerin listesi */
	public List<Entity> getShinyEntities() {
		return shinyEntities;
	}
	
	/** @return Suyun altındayken (kırılma etkisiyle) görünen objelerin listesi */
	public List<Entity> getUnderwaterEntities() {
		return underwaterEntities;
	}

	/** @return Sahnedeki standart tüm nesnelerin listesi */
	public List<Entity> getAllEntities() {
		return standardEntities;
	}

	/**
	 * Sahne kapatılırken gökyüzünü, tüm nesneleri ve yansıma haritasını GPU belleğinden tamamen temizler.
	 */
	public void delete() {
		if (sky != null) {
			sky.delete();
		}
		for (Entity entity : standardEntities) {
			entity.delete();
		}
		environmentMap.delete();
	}

}
