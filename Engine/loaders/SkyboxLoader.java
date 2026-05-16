package loaders;

import skybox.Skybox;
import textures.Texture;
import utils.MyFile;

/**
 * Gökyüzü küpünü (Skybox) yüklemekten sorumlu sınıf.
 */
public class SkyboxLoader {

	/**
	 * Belirtilen klasördeki 6 gökyüzü dokusunu okuyup yeni bir Skybox nesnesi oluşturur.
	 * 
	 * @param skyboxFolder Gökyüzü dokularının bulunduğu klasör
	 * @return Yüklenen Skybox
	 */
	protected Skybox loadSkyBox(MyFile skyboxFolder) {
		MyFile[] textureFiles = getSkyboxTexFiles(skyboxFolder);
		Texture cubeMap = Texture.newCubeMap(textureFiles);
		return new Skybox(cubeMap, LoaderSettings.SKYBOX_SIZE);
	}

	/**
	 * Belirtilen klasör içerisindeki Skybox yüzey kaplamalarının MyFile array halini döndürür.
	 * Dosya isimleri LoaderSettings sınıfından alınır.
	 * 
	 * @param skyboxFolder Gökyüzü dokularının bulunduğu klasör
	 * @return Dosyaların listesi
	 */
	private MyFile[] getSkyboxTexFiles(MyFile skyboxFolder) {
		MyFile[] files = new MyFile[LoaderSettings.SKYBOX_TEX_FILES.length];
		for (int i = 0; i < files.length; i++) {
			files[i] = new MyFile(skyboxFolder, LoaderSettings.SKYBOX_TEX_FILES[i]);
		}
		return files;
	}

}
