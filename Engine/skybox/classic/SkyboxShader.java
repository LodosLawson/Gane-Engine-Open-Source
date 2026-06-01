package skybox.classic;

import shaders.ShaderProgram;
import shaders.UniformMatrix;
import utils.MyFile;

/**
 * GÃ¶kyÃ¼zÃ¼ (Skybox) Ã§izimi iÃ§in Ã¶zelleÅŸtirilmiÅŸ shader programÄ±.
 * Sadece pozisyon verisi (in_position) kullanÄ±r (Normal veya TextureCoord kullanmaz).
 */
public class SkyboxShader extends ShaderProgram {

	private static final MyFile VERTEX_SHADER = new MyFile("skybox/classic", "skyboxVertex.txt");
	private static final MyFile FRAGMENT_SHADER = new MyFile("skybox/classic", "skyboxFragment.txt");

	// KameranÄ±n bakÄ±ÅŸ aÃ§Ä±sÄ±nÄ± gÃ¶kyÃ¼zÃ¼ kÃ¼pÃ¼ne uygulayan matris
	protected UniformMatrix projectionViewMatrix = new UniformMatrix("projectionViewMatrix");

	/**
	 * GÃ¶kyÃ¼zÃ¼ shader programÄ±nÄ± derler ve gerekli Uniform deÄŸiÅŸkenleri baÄŸlar.
	 */
	public SkyboxShader() {
		// YalnÄ±zca 0. indekste bulunan 'in_position' (kÃ¶ÅŸe koordinatlarÄ±) gereklidir
		super(VERTEX_SHADER, FRAGMENT_SHADER, "in_position");
		super.storeAllUniformLocations(projectionViewMatrix);
	}
}


