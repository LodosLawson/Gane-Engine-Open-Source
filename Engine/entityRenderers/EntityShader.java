package entityRenderers;

import shaders.ShaderProgram;
import shaders.UniformBoolean;
import shaders.UniformMatrix;
import shaders.UniformSampler;
import shaders.UniformVec3;
import shaders.UniformVec4;
import shaders.UniformFloat;
import utils.MyFile;

/**
 * EntityRenderer için kullanılan özel Shader programını temsil eder.
 * Ekrandaki nesnelerin GLSL dosyaları aracılığıyla nasıl boyanacağını ve işleneceğini yönetir.
 */
public class EntityShader extends ShaderProgram {

	/**
	 * Köşe (Vertex) shader dosyasının yolu. Nesnenin 3D uzaydaki konumlarını hesaplar.
	 */
	private static final MyFile VERTEX_SHADER = new MyFile("entityRenderers", "entityVertex.txt");
	
	/**
	 * Parça (Fragment) shader dosyasının yolu. Nesnenin ekrandaki piksellerinin rengini hesaplar.
	 */
	private static final MyFile FRAGMENT_SHADER = new MyFile("entityRenderers",
			"entityFragment.txt");

	/**
	 * Kameranın ve dünyanın konumunu hesaplamak için kullanılan matris değişkeni.
	 */
	protected UniformMatrix projectionViewMatrix = new UniformMatrix("projectionViewMatrix");
	
	/**
	 * Modelde ekstra bir kaplama haritası olup olmadığını belirten boolean (mantıksal) değişken.
	 */
	protected UniformBoolean hasExtraMap = new UniformBoolean("hasExtraMap");
	
	/**
	 * Nesnenin dünyadaki konumunu (translation) hesaplamak için matris.
	 */
	protected UniformMatrix transformationMatrix = new UniformMatrix("transformationMatrix");
	
	/**
	 * Işığın (örneğin güneşin) geliş yönünü tutan 3 boyutlu vektör değişkeni.
	 */
	protected UniformVec3 lightDirection = new UniformVec3("lightDirection");
	protected UniformVec3 lightColor = new UniformVec3("lightColor");
	protected UniformFloat lightBrightness = new UniformFloat("lightBrightness");
	protected UniformFloat ambientLight = new UniformFloat("ambientLight");
	
	/**
	 * Kırpma düzlemi vektörü. Örneğin su altı yansımalarında ekranın bir kısmını kesmek için kullanılır.
	 */
	protected UniformVec4 plane = new UniformVec4("plane");

	// Nokta Işık (Point Light) Uniformları
	protected UniformVec3 pointLightPos = new UniformVec3("pointLightPos");
	protected UniformVec3 pointLightColor = new UniformVec3("pointLightColor");
	protected UniformVec3 pointLightAttenuation = new UniformVec3("pointLightAttenuation");

	/**
	 * Modelin ana kaplamasını (resmini) tutan sampler.
	 */
	private UniformSampler diffuseMap = new UniformSampler("diffuseMap");
	
	/**
	 * Modelin ekstra kaplamasını (örneğin parlayan alanlar) tutan sampler.
	 */
	private UniformSampler extraMap = new UniformSampler("extraMap");

	protected UniformVec3 cameraPosition = new UniformVec3("cameraPosition");

	/**
	 * Shader programını başlatan ve değişkenleri bağlayan yapıcı (constructor) metot.
	 */
	public EntityShader() {
		super(VERTEX_SHADER, FRAGMENT_SHADER, "in_position", "in_textureCoords", "in_normal");
		super.storeAllUniformLocations(projectionViewMatrix, transformationMatrix, diffuseMap, extraMap, hasExtraMap,
				lightDirection, lightColor, lightBrightness, ambientLight, plane, pointLightPos, pointLightColor, pointLightAttenuation, cameraPosition);
		connectTextureUnits();
	}

	/**
	 * Texture (kaplama) ünitelerini shader üzerindeki değişkenlere bağlar.
	 * Neden: GLSL içindeki değişkenlerin hangi kaplamayı (0 veya 1 numaralı) kullanacağını belirtmek içindir.
	 */
	private void connectTextureUnits() {
		super.start();
		diffuseMap.loadTexUnit(0);
		extraMap.loadTexUnit(1);
		super.stop();
	}

}
