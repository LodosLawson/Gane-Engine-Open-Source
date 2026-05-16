package loaders;

import utils.MyFile;

/**
 * Dosya yükleme işlemleri sırasında kullanılacak sabit değişkenleri barındıran ayar sınıfı.
 */
public class LoaderSettings {
	
	// Kaynak dosyalarının bulunduğu ana klasör
	public static final MyFile RES_FOLDER = new MyFile("res");
	
	// Varlıkların (Entity) bulunduğu klasör adı
	protected static final String ENTITIES_FOLDER = "entities";
	// Gökyüzü (Skybox) dokularının bulunduğu klasör adı
	protected static final String SKYBOX_FOLDER = "skybox";
	// Gökyüzü küp haritası (cubemap) için gereken 6 adet dokunun dosya adları
	protected static final String[] SKYBOX_TEX_FILES = {"posX.png", "negX.png", "posY.png", "negY.png", "posZ.png", "negZ.png"};
	// Gökyüzünün boyutu
	protected static final float SKYBOX_SIZE = 100;
	// Sahnede yer alacak varlıkların listesini tutan dosya
	protected static final String ENTITY_LIST_FILE = "entityList.txt";
	// Model ayarlarını barındıran dosyanın adı
	protected static final String CONFIGS_FILE = "configs.txt";
	// Model 3B veri dosyasının adı
	protected static final String MODEL_FILE = "model.obj";
	// Modelin temel renk (diffuse) dokusunun dosya adı
	protected static final String DIFFUSE_FILE = "diffuse.png";
	// Ekstra harita dokusunun (parlaklık, normal map vb.) dosya adı
	protected static final String EXTRA_MAP_FILE = "extra.png";
	
	// Ayar dosyalarındaki anahtar-değer ayracı
	protected static final String SEPARATOR = ";";
	// Doğru/Evet anlamında kullanılan sabit ifade
	protected static final String TRUE = "TRUE";
	
}
