package skybox.classic;

import skybox.ISky;

import org.lwjgl.util.vector.Vector3f;
import openglObjects.Vao;
import textures.Texture;
import utils.ICamera;

/**
 * Sahnenin arka planÄ±nÄ± (GÃ¶kyÃ¼zÃ¼nÃ¼) temsil eden sÄ±nÄ±f.
 * Geometrik bir kÃ¼p (VAO) ve 360 derecelik panoramik (Equirectangular) dokudan oluÅŸur.
 */
public class Skybox implements ISky {

	private static final float SIZE = 150f;

	private Vao cube;
	private Texture texture;

	private static SkyboxRenderer sharedRenderer;

	public Skybox(Texture texture) {
		this.cube = CubeGenerator.generateCube(SIZE);
		this.texture = texture;
		ensureRenderer();
	}

	@Override
	public void cleanUp() {
		// EÄŸer texture veya cube temizlenmesi gerekirse burada yapÄ±lÄ±r.
	}
	
	/**
	 * Yeni bir GÃ¶kyÃ¼zÃ¼ Kutusu (Skybox) oluÅŸturur.
	 * 
	 * @param panoramaTexture GÃ¶kyÃ¼zÃ¼ kaplamasÄ± (Equirectangular 2D Texture)
	 * @param size KÃ¼pÃ¼n boyutu (Genellikle kameranÄ±n gÃ¶rÃ¼ÅŸ mesafesini kapsayacak kadar bÃ¼yÃ¼k olmalÄ±dÄ±r)
	 */
	public Skybox(Texture panoramaTexture, float size){
		cube = CubeGenerator.generateCube(size);
		this.texture = panoramaTexture;
		ensureRenderer();
	}

	private void ensureRenderer() {
		if (sharedRenderer == null) sharedRenderer = new SkyboxRenderer();
	}

	/**
	 * ISky arayÃ¼zÃ¼: MasterRenderer tarafÄ±ndan polymorphic olarak Ã§aÄŸrÄ±lÄ±r.
	 */
	@Override
	public void render(ICamera camera, Vector3f lightDir) {
		if (sharedRenderer != null) sharedRenderer.render(this, camera);
	}
	
	/** @return GÃ¶kyÃ¼zÃ¼nÃ¼n geometrisini (KÃ¼p) dÃ¶ndÃ¼rÃ¼r */
	public Vao getCubeVao(){
		return cube;
	}
	
	/** @return GÃ¶kyÃ¼zÃ¼nÃ¼n panoramik kaplama dokusunu (2D Texture) dÃ¶ndÃ¼rÃ¼r */
	public Texture getTexture(){
		return texture;
	}
	
	/** GÃ¶kyÃ¼zÃ¼nÃ¼n VAO ve doku verilerini ekran kartÄ± belleÄŸinden temizler */
	public void delete(){
		cube.delete();
		texture.delete();
	}

}

