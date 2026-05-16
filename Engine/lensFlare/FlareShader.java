package lensFlare;

import shaders.ShaderProgram;
import shaders.UniformFloat;
import shaders.UniformSampler;
import shaders.UniformVec4;
import utils.MyFile;

/**
 * Mercek parlamasını (lens flare) render etmek için kullanılan shader programını ayarlar.
 * 3 uniform değişkenin konumunu alır, "in_position" değişkenini VAO'nun 0. attribute'uyla bağlar
 * ve sampler uniform'unu 0 numaralı doku ünitesine (texture unit) bağlar.
 * 
 * @author Karl
 */
public class FlareShader extends ShaderProgram {

	// Vertex ve Fragment shader dosyalarının yolları
	private static final MyFile VERTEX_SHADER = new MyFile("lensFlare", "flareVertex.glsl");
	private static final MyFile FRAGMENT_SHADER = new MyFile("lensFlare", "flareFragment.glsl");

	// Parlaklık değerini shadere yollamak için uniform
	protected UniformFloat brightness = new UniformFloat("brightness");
	// Ekrandaki dönüşüm bilgisini (konum ve ölçek) taşıyan uniform
	protected UniformVec4 transform = new UniformVec4("transform");

	// Parlama dokusu (texture) sampler'ı
	private UniformSampler flareTexture = new UniformSampler("flareTexture");

	/**
	 * Shader programını başlatır, dosyaları yükler ve uniform'ların bağlantılarını yapar.
	 */
	public FlareShader() {
		super(VERTEX_SHADER, FRAGMENT_SHADER, "in_position");
		super.storeAllUniformLocations(brightness, flareTexture, transform);
		connectTextureUnits();
	}

	/**
	 * Parlama dokusunu shader içerisindeki 0. doku birimine bağlar.
	 */
	private void connectTextureUnits() {
		super.start();
		flareTexture.loadTexUnit(0);
		super.stop();
	}

}
