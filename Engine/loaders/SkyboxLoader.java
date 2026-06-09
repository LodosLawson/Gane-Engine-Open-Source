package loaders;

import skybox.classic.Skybox;
import textures.Texture;
import utils.MyFile;

/**
 * GÃ¶kyÃ¼zÃ¼ kÃ¼pÃ¼nÃ¼ (Skybox) yÃ¼klemekten sorumlu sÄ±nÄ±f.
 */
public class SkyboxLoader {

	/**
	 * Belirtilen klasÃ¶rdeki 6 gÃ¶kyÃ¼zÃ¼ dokusunu okuyup yeni bir Skybox nesnesi oluÅŸturur.
	 * 
	 * @param skyboxFolder GÃ¶kyÃ¼zÃ¼ dokularÄ±nÄ±n bulunduÄŸu klasÃ¶r
	 * @return YÃ¼klenen Skybox
	 */
	protected Skybox loadSkyBox(MyFile skyboxFolder) {
		MyFile[] textureFiles = getSkyboxTexFiles(skyboxFolder);
		Texture cubeMap = Texture.newCubeMap(textureFiles);
		return new Skybox(cubeMap, LoaderSettings.SKYBOX_SIZE);
	}

	/**
	 * Belirtilen klasÃ¶r iÃ§erisindeki Skybox yÃ¼zey kaplamalarÄ±nÄ±n MyFile array halini dÃ¶ndÃ¼rÃ¼r.
	 * Dosya isimleri LoaderSettings sÄ±nÄ±fÄ±ndan alÄ±nÄ±r.
	 * 
	 * @param skyboxFolder GÃ¶kyÃ¼zÃ¼ dokularÄ±nÄ±n bulunduÄŸu klasÃ¶r
	 * @return DosyalarÄ±n listesi
	 */
	private MyFile[] getSkyboxTexFiles(MyFile skyboxFolder) {
		MyFile[] files = new MyFile[LoaderSettings.SKYBOX_TEX_FILES.length];
		for (int i = 0; i < files.length; i++) {
			files[i] = new MyFile(skyboxFolder, LoaderSettings.SKYBOX_TEX_FILES[i]);
		}
		return files;
	}

}

