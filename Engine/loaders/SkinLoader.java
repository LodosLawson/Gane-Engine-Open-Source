package loaders;

import scene.Skin;
import textures.Texture;
import utils.MyFile;

/**
 * Objelerin kaplamalarını (Skin - Diffuse, Extra vb. Textures) yükler.
 */
public class SkinLoader {
	
	/**
	 * Sadece temel renk (diffuse) dokusuna sahip bir Skin yükler.
	 * 
	 * @param diffuseFile Yüklenecek olan temel doku dosyası
	 * @return Oluşturulan Skin nesnesi
	 */
	public Skin loadSkin(MyFile diffuseFile){
		Texture diffuse = Texture.newTexture(diffuseFile).anisotropic().create();
		return new Skin(diffuse, null);
	}
	
	/**
	 * Hem temel dokuya (diffuse) hem de ekstra haritaya (extra map) sahip bir Skin yükler.
	 * 
	 * @param diffuseFile Temel doku dosyası
	 * @param extraMapFile Ekstra bilgi barındıran doku (ör: normal map, parlaklık haritası)
	 * @return Oluşturulan Skin nesnesi
	 */
	public Skin loadSkin(MyFile diffuseFile, MyFile extraMapFile){
		Texture diffuse = Texture.newTexture(diffuseFile).anisotropic().create();
		Texture extraMap = Texture.newTexture(extraMapFile).create();
		return new Skin(diffuse, extraMap);
	}

}
