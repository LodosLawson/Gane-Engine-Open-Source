package loaders;

import scene.Entity;
import scene.Model;
import scene.Skin;
import utils.MyFile;

/**
 * Tek bir varlığı (Entity); modelini, ayarlarını ve kaplamasını (skin) okuyup 
 * bir araya getirerek oluşturan sınıf.
 */
public class EntityLoader {
	
	private ModelLoader modelLoader;
	private SkinLoader skinLoader;
	private ConfigsLoader configsLoader;
	
	/**
	 * Varlıkları oluşturmak için gerekli alt yükleyici sınıflarını alır.
	 * 
	 * @param modelLoader Model veri yükleyicisi
	 * @param skinLoader Kaplama (doku) yükleyicisi
	 * @param configsLoader Ayar okuyucusu
	 */
	public EntityLoader(ModelLoader modelLoader, SkinLoader skinLoader, ConfigsLoader configsLoader){
		this.modelLoader = modelLoader;
		this.skinLoader = skinLoader;
		this.configsLoader = configsLoader;
	}
	
	/**
	 * Belirtilen klasördeki nesnenin tüm bileşenlerini (model.obj, configs.txt, diffuse.png) 
	 * yükleyerek yeni bir Entity oluşturur.
	 * 
	 * @param entityFile Yüklenecek olan nesnenin kök dizini (MyFile nesnesi)
	 * @return Oluşturulan ve ayarları yapılmış Entity
	 */
	public Entity loadEntity(MyFile entityFile){
		Model model = modelLoader.loadModel(new MyFile(entityFile, LoaderSettings.MODEL_FILE));
		Configs configs = configsLoader.loadConfigs(new MyFile(entityFile, LoaderSettings.CONFIGS_FILE));
		Skin skin = loadSkin(entityFile, configs);
		Entity entity = new Entity(model, skin);
		setEntityConfigs(entity, configs);
		return entity;
	}
	
	/**
	 * Objeye ait kaplamayı, konfigürasyondaki ayarlara bakarak (ekstra harita var/yok) yükler.
	 * 
	 * @param entityFile Objeyi barındıran klasör
	 * @param configs Objenin config dosyası özellikleri
	 * @return Yüklenen kaplama objesi (Skin)
	 */
	private Skin loadSkin(MyFile entityFile, Configs configs){
		Skin skin = null;
		MyFile diffuseFile = new MyFile(entityFile, LoaderSettings.DIFFUSE_FILE);
		if(configs.hasExtraMap()){
			skin = skinLoader.loadSkin(diffuseFile, new MyFile(entityFile, LoaderSettings.EXTRA_MAP_FILE));
		}else{
			skin = skinLoader.loadSkin(diffuseFile);
		}
		// Şeffaflık ayarını skin'e uygula
		skin.setTransparent(configs.hasTransparency());
		return skin;
	}
	
	/**
	 * Config dosyasında okunan ayarları Entity objesine uygular.
	 * 
	 * @param entity Ayarları uygulanacak Entity
	 * @param configs Uygulanacak ayar nesnesi
	 */
	private void setEntityConfigs(Entity entity, Configs configs){
		entity.setCastsShadow(configs.castsShadow());
		entity.setHasReflection(configs.hasReflection());
		entity.setSeenUnderWater(configs.hasRefraction());
		entity.setImportant(configs.isImportant());
	}

}
