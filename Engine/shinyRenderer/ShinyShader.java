package shinyRenderer;

import shaders.ShaderProgram;
import shaders.UniformMatrix;
import shaders.UniformSampler;
import shaders.UniformVec3;
import utils.MyFile;

/**
 * Parlak nesnelerin (çevresel yansıma yapan nesnelerin) render edilmesinde kullanılan
 * özel shader programını yönetir. Çevresel haritayı (Environment Map) ve ışık yansımalarını (Specular/Reflection) hesaplar.
 */
public class ShinyShader extends ShaderProgram {

	private static final MyFile VERTEX_SHADER = new MyFile("shinyRenderer", "shinyVertex.txt");
	private static final MyFile FRAGMENT_SHADER = new MyFile("shinyRenderer", "shinyFragment.txt");

	// Kameranın bakış açısını nesneye uygulayan dönüşüm matrisi
	protected UniformMatrix projectionViewMatrix = new UniformMatrix("projectionViewMatrix");
	// Fresnel (Yansıma şiddeti) hesaplaması için kameranın uzaydaki konumu
	protected UniformVec3 cameraPosition = new UniformVec3("cameraPosition");
	// Işık yansımasını (Specular) hesaplamak için ışık yönü
	protected UniformVec3 lightDirection = new UniformVec3("lightDirection");

	// Objenin temel rengi/kaplaması
	private UniformSampler diffuseMap = new UniformSampler("diffuseMap");
	// Objenin yüzeyinde yansıyacak olan çevresel küp haritası (Environment Map)
	private UniformSampler enviroMap = new UniformSampler("enviroMap");

	/**
	 * Parlak nesneler için özel shader'ı derler ve uniform değişkenleri bağlar.
	 */
	public ShinyShader() {
		super(VERTEX_SHADER, FRAGMENT_SHADER, "in_position", "in_textureCoords", "in_normal");
		super.storeAllUniformLocations(projectionViewMatrix, diffuseMap, cameraPosition, lightDirection, enviroMap);
		connectTextureUnits();
	}

	/**
	 * Kaplama (Diffuse) ve Çevre Yansıması (Environment Map) için doğru
	 * doku birimlerini (Texture Units) ayarlar.
	 */
	private void connectTextureUnits() {
		super.start();
		diffuseMap.loadTexUnit(0);
		enviroMap.loadTexUnit(1);
		super.stop();
	}

}
