package skybox;

import shaders.ShaderProgram;
import shaders.UniformMatrix;
import utils.MyFile;

/**
 * Gökyüzü (Skybox) çizimi için özelleştirilmiş shader programı.
 * Sadece pozisyon verisi (in_position) kullanır (Normal veya TextureCoord kullanmaz).
 */
public class SkyboxShader extends ShaderProgram {

	private static final MyFile VERTEX_SHADER = new MyFile("skybox", "skyboxVertex.txt");
	private static final MyFile FRAGMENT_SHADER = new MyFile("skybox", "skyboxFragment.txt");

	// Kameranın bakış açısını gökyüzü küpüne uygulayan matris
	protected UniformMatrix projectionViewMatrix = new UniformMatrix("projectionViewMatrix");

	/**
	 * Gökyüzü shader programını derler ve gerekli Uniform değişkenleri bağlar.
	 */
	public SkyboxShader() {
		// Yalnızca 0. indekste bulunan 'in_position' (köşe koordinatları) gereklidir
		super(VERTEX_SHADER, FRAGMENT_SHADER, "in_position");
		super.storeAllUniformLocations(projectionViewMatrix);
	}
}
