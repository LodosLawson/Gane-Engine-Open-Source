package loaders;

import java.io.BufferedReader;
import java.io.IOException;

import extra.Camera;
import gane.WorldSettings; 
import scene.Entity;
import scene.Scene;
import skybox.Skybox;
import utils.ICamera;
import utils.MyFile;

/**
 * Oyun sahnesini (Scene) dışarıdan yüklemekle sorumlu sınıf.
 * Sahnedeki modelleri, araziyi, parlak objeleri ve gökyüzünü oluşturarak geri döndürür.
 */
public class SceneLoader {

	private EntityLoader entityLoader;
	private SkyboxLoader skyLoader;

	/**
	 * SceneLoader'ı ilklendirir.
	 * 
	 * @param entityLoader Nesneleri (Entity) yükleyen alt birim
	 * @param skyLoader Gökyüzü (Skybox) yükleyen alt birim
	 */
	public SceneLoader(EntityLoader entityLoader, SkyboxLoader skyLoader) {
		this.entityLoader = entityLoader;
		this.skyLoader = skyLoader;
	}

	/**
	 * Belirtilen klasördeki sahne bilgilerini (entityList.txt vb.) okuyarak Scene nesnesini oluşturur.
	 * 
	 * @param sceneFile Sahne bilgilerini içeren kök dizin
	 * @return Tamamlanmış ve ayarlanmış yeni Scene
	 */
	public Scene loadScene(MyFile sceneFile) {
		// Sahne içerisindeki nesne listesini oku
		MyFile sceneList = new MyFile(sceneFile, LoaderSettings.ENTITY_LIST_FILE);
		BufferedReader reader = getReader(sceneList);
		
		// Arazi, parlak objeler ve standart objelerin dosyalarını listele
		MyFile[] terrainFiles = readEntityFiles(reader, sceneFile);
		MyFile[] shinyFiles = readEntityFiles(reader, sceneFile);
		MyFile[] entityFiles = readEntityFiles(reader, sceneFile);
		closeReader(reader);
		
		// Gökyüzü dokularını yükle
		Skybox sky = skyLoader.loadSkyBox(new MyFile(sceneFile, LoaderSettings.SKYBOX_FOLDER));
		
		return createScene(terrainFiles, entityFiles, shinyFiles, sky);
	}

	/**
	 * Yüklenmiş dosyalardan yararlanarak sahneyi ayağa kaldırır, kamerayı ekler.
	 * 
	 * @param terrainFiles Yeryüzü şekilleri ve objeleri
	 * @param entityFiles Sahnedeki sıradan objeler
	 * @param shinyFiles Sahnede yansıma yapacak olan objeler
	 * @param sky Sahneye ait gökyüzü
	 * @return Sahne objesi
	 */
	private Scene createScene(MyFile[] terrainFiles, MyFile[] entityFiles, MyFile[] shinyFiles, Skybox sky){
		ICamera camera = new Camera();
		Scene scene = new Scene(camera, sky);
		scene.setLightDirection(WorldSettings.LIGHT_DIR);
		
		addEntities(scene, entityFiles);
		addShinyEntities(scene, shinyFiles);
		addTerrains(scene, terrainFiles);
		
		return scene;
	}
	
	/** Sahneye standart Entity'leri yükleyip ekler. */
	private void addEntities(Scene scene, MyFile[] entityFiles){
		for(MyFile file : entityFiles){
			Entity entity = entityLoader.loadEntity(file);
			scene.addEntity(entity);
		}
	}
	
	/** Sahneye parlak (shiny) Entity'leri yükleyip ekler. */
	private void addShinyEntities(Scene scene, MyFile[] entityFiles){
		for(MyFile file : entityFiles){
			Entity entity = entityLoader.loadEntity(file);
			scene.addShiny(entity);
		}
	}
	
	/** Sahneye arazi (terrain) Entity'lerini yükleyip ekler. */
	private void addTerrains(Scene scene, MyFile[] terrainFiles){
		for(MyFile file : terrainFiles){
			Entity entity = entityLoader.loadEntity(file);
			scene.addTerrain(entity);
		}
	}
	
	/**
	 * Bir dosyayı okuyacak olan BufferedReader objesini döndürür.
	 * Dosya bulunamazsa hatayı yazıp programı sonlandırır.
	 */
	private BufferedReader getReader(MyFile file) {
		try {
			return file.getReader();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Couldn't find scene file: " + file);
			System.exit(-1);
			return null;
		}
	}
	
	/** BufferedReader'ı kapatır. */
	private void closeReader(BufferedReader reader){
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Listede belirtilen klasör isimlerini okur ve MyFile dizisi olarak döndürür.
	 * 
	 * @param reader Satırları okuyan nesne
	 * @param sceneFile Kök dizin objesi
	 * @return Liste satırındaki ayrıştırılmış dosyalar
	 */
	private MyFile[] readEntityFiles(BufferedReader reader, MyFile sceneFile) {
		try {
			String line = reader.readLine();
			String[] names = line.split(LoaderSettings.SEPARATOR);
			MyFile[] files = new MyFile[names.length];
			for(int i=0;i<files.length;i++){
				files[i] = new MyFile(sceneFile, names[i]);
			}
			return files;
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Couldn't read scene file: "+sceneFile);
			System.exit(-1);
			return null;
		}
	}

}
