package loaders;

import java.io.BufferedReader;
import java.io.IOException;

import extra.Camera;
import gane.WorldSettings; 
import scene.Entity;
import scene.Scene;
import skybox.classic.Skybox;
import utils.ICamera;
import utils.MyFile;

/**
 * Oyun sahnesini (Scene) dÄ±ÅŸarÄ±dan yÃ¼klemekle sorumlu sÄ±nÄ±f.
 * Sahnedeki modelleri, araziyi, parlak objeleri ve gÃ¶kyÃ¼zÃ¼nÃ¼ oluÅŸturarak geri dÃ¶ndÃ¼rÃ¼r.
 */
public class SceneLoader {

	private EntityLoader entityLoader;
	private SkyboxLoader skyLoader;

	/**
	 * SceneLoader'Ä± ilklendirir.
	 * 
	 * @param entityLoader Nesneleri (Entity) yÃ¼kleyen alt birim
	 * @param skyLoader GÃ¶kyÃ¼zÃ¼ (Skybox) yÃ¼kleyen alt birim
	 */
	public SceneLoader(EntityLoader entityLoader, SkyboxLoader skyLoader) {
		this.entityLoader = entityLoader;
		this.skyLoader = skyLoader;
	}

	/**
	 * Belirtilen klasÃ¶rdeki sahne bilgilerini (entityList.txt vb.) okuyarak Scene nesnesini oluÅŸturur.
	 * 
	 * @param sceneFile Sahne bilgilerini iÃ§eren kÃ¶k dizin
	 * @return TamamlanmÄ±ÅŸ ve ayarlanmÄ±ÅŸ yeni Scene
	 */
	public Scene loadScene(MyFile sceneFile) {
		// Sahne iÃ§erisindeki nesne listesini oku
		MyFile sceneList = new MyFile(sceneFile, LoaderSettings.ENTITY_LIST_FILE);
		BufferedReader reader = getReader(sceneList);
		
		// Arazi, parlak objeler ve standart objelerin dosyalarÄ±nÄ± listele
		MyFile[] terrainFiles = readEntityFiles(reader, sceneFile);
		MyFile[] shinyFiles = readEntityFiles(reader, sceneFile);
		MyFile[] entityFiles = readEntityFiles(reader, sceneFile);
		closeReader(reader);
		
		// GÃ¶kyÃ¼zÃ¼ dokularÄ±nÄ± yÃ¼kle
		Skybox sky = skyLoader.loadSkyBox(new MyFile(sceneFile, LoaderSettings.SKYBOX_FOLDER));
		
		return createScene(terrainFiles, entityFiles, shinyFiles, sky);
	}

	/**
	 * YÃ¼klenmiÅŸ dosyalardan yararlanarak sahneyi ayaÄŸa kaldÄ±rÄ±r, kamerayÄ± ekler.
	 * 
	 * @param terrainFiles YeryÃ¼zÃ¼ ÅŸekilleri ve objeleri
	 * @param entityFiles Sahnedeki sÄ±radan objeler
	 * @param shinyFiles Sahnede yansÄ±ma yapacak olan objeler
	 * @param sky Sahneye ait gÃ¶kyÃ¼zÃ¼
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
	
	/** Sahneye standart Entity'leri yÃ¼kleyip ekler. */
	private void addEntities(Scene scene, MyFile[] entityFiles){
		for(MyFile file : entityFiles){
			Entity entity = entityLoader.loadEntity(file);
			scene.addEntity(entity);
		}
	}
	
	/** Sahneye parlak (shiny) Entity'leri yÃ¼kleyip ekler. */
	private void addShinyEntities(Scene scene, MyFile[] entityFiles){
		for(MyFile file : entityFiles){
			Entity entity = entityLoader.loadEntity(file);
			scene.addShiny(entity);
		}
	}
	
	/** Sahneye arazi (terrain) Entity'lerini yÃ¼kleyip ekler. */
	private void addTerrains(Scene scene, MyFile[] terrainFiles){
		for(MyFile file : terrainFiles){
			Entity entity = entityLoader.loadEntity(file);
			scene.addTerrainEntity(entity);
		}
	}
	
	/**
	 * Bir dosyayÄ± okuyacak olan BufferedReader objesini dÃ¶ndÃ¼rÃ¼r.
	 * Dosya bulunamazsa hatayÄ± yazÄ±p programÄ± sonlandÄ±rÄ±r.
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
	
	/** BufferedReader'Ä± kapatÄ±r. */
	private void closeReader(BufferedReader reader){
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Listede belirtilen klasÃ¶r isimlerini okur ve MyFile dizisi olarak dÃ¶ndÃ¼rÃ¼r.
	 * 
	 * @param reader SatÄ±rlarÄ± okuyan nesne
	 * @param sceneFile KÃ¶k dizin objesi
	 * @return Liste satÄ±rÄ±ndaki ayrÄ±ÅŸtÄ±rÄ±lmÄ±ÅŸ dosyalar
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

