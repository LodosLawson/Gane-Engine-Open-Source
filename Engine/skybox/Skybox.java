package skybox;

import openglObjects.Vao;
import textures.Texture;

/**
 * Sahnenin arka planını (Gökyüzünü) temsil eden sınıf.
 * Geometrik bir küp (VAO) ve bu küpün üzerine kaplanacak bir 360 derecelik
 * dokudan (CubeMap Texture) oluşur.
 */
public class Skybox {
	
	// Gökyüzü geometrisi (Küp modeli)
	private Vao cube;
	// Küpün üzerine kaplanacak doku (CubeMap)
	private Texture texture;
	
	/**
	 * Yeni bir Gökyüzü Kutusu (Skybox) oluşturur.
	 * 
	 * @param cubeMapTexture Gökyüzü kaplaması (Texture)
	 * @param size Küpün boyutu (Genellikle kameranın görüş mesafesini kapsayacak kadar büyük olmalıdır)
	 */
	public Skybox(Texture cubeMapTexture, float size){
		cube = CubeGenerator.generateCube(size);
		this.texture = cubeMapTexture;
	}
	
	/** @return Gökyüzünün geometrisini (Küp) döndürür */
	public Vao getCubeVao(){
		return cube;
	}
	
	/** @return Gökyüzünün kaplama dokusunu (CubeMap) döndürür */
	public Texture getTexture(){
		return texture;
	}
	
	/** Gökyüzünün VAO ve doku verilerini ekran kartı belleğinden temizler */
	public void delete(){
		cube.delete();
		texture.delete();
	}

}
