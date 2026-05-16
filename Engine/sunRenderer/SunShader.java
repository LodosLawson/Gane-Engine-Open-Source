package sunRenderer;

import shaders.ShaderProgram;
import shaders.UniformMatrix;
import shaders.UniformSampler;
import utils.MyFile;

/**
 * Güneş çizimi için özelleştirilmiş shader programı.
 * Güneşin 2B dokusunu (texture) ve konum/yönelim matrisini (MVP) yönetir.
 */
public class SunShader extends ShaderProgram {

	private static final MyFile VERTEX_SHADER = new MyFile("sunRenderer", "sunVertex.glsl");
	private static final MyFile FRAGMENT_SHADER = new MyFile("sunRenderer", "sunFragment.glsl");
	
	// Güneş kaplamasını shader'a bağlayan uniform değişkeni (Sampler2D)
	protected UniformSampler sunTexture = new UniformSampler("sunTexture");
	// Model-View-Projection matrisini shader'a gönderen uniform değişkeni
	protected UniformMatrix mvpMatrix = new UniformMatrix("mvpMatrix");

	/**
	 * Güneş shader'ını derler ve gerekli uniform değişkenleri bağlar.
	 */
	public SunShader() {
		// "in_position" özelliği, dörtgenin (quad) köşe koordinatlarını shader'a aktarır
		super(VERTEX_SHADER, FRAGMENT_SHADER, "in_position");
		super.storeAllUniformLocations(sunTexture, mvpMatrix);
		connectTextureUnits();
	}

	/**
	 * Texture ünitelerini (Texture units) shader üzerindeki sampler değişkenleriyle eşleştirir.
	 */
	private void connectTextureUnits() {
		super.start();
		// Güneş kaplamasının 0. doku biriminden (Texture Unit 0) okunacağını belirtiriz
		sunTexture.loadTexUnit(0);
		super.stop();
	}

}
