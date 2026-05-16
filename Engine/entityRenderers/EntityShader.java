package entityRenderers;

import shaders.ShaderProgram;
import shaders.UniformBoolean;
import shaders.UniformMatrix;
import shaders.UniformSampler;
import shaders.UniformVec3;
import shaders.UniformVec4;
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
	 * Işığın (örneğin güneşin) geliş yönünü tutan 3 boyutlu vektör değişkeni.
	 */
	protected UniformVec3 lightDirection = new UniformVec3("lightDirection");
	
	/**
	 * Kırpma düzlemi vektörü. Örneğin su altı yansımalarında ekranın bir kısmını kesmek için kullanılır.
	 */
	protected UniformVec4 plane = new UniformVec4("plane");

	/**
	 * Modelin ana kaplamasını (resmini) tutan sampler.
	 */
	private UniformSampler diffuseMap = new UniformSampler("diffuseMap");
	
	/**
	 * Modelin ekstra kaplamasını (örneğin parlayan alanlar) tutan sampler.
	 */
	private UniformSampler extraMap = new UniformSampler("extraMap");

	/**
	 * Shader programını başlatan ve değişkenleri bağlayan yapıcı (constructor) metot.
	 */
	public EntityShader() {
		super(VERTEX_SHADER, FRAGMENT_SHADER, "in_position", "in_textureCoords", "in_normal");
		super.storeAllUniformLocations(projectionViewMatrix, diffuseMap, extraMap, hasExtraMap,
				lightDirection, plane);
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
