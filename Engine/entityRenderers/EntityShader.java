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
 * EntityRenderer iÃ§in kullanÄ±lan Ã¶zel Shader programÄ±nÄ± temsil eder.
 * Ekrandaki nesnelerin GLSL dosyalarÄ± aracÄ±lÄ±ÄŸÄ±yla nasÄ±l boyanacaÄŸÄ±nÄ± ve iÅŸleneceÄŸini yÃ¶netir.
 */
public class EntityShader extends ShaderProgram {

	/**
	 * KÃ¶ÅŸe (Vertex) shader dosyasÄ±nÄ±n yolu. Nesnenin 3D uzaydaki konumlarÄ±nÄ± hesaplar.
	 */
	private static final MyFile VERTEX_SHADER = new MyFile("entityRenderers", "entityVertex.txt");
	
	/**
	 * ParÃ§a (Fragment) shader dosyasÄ±nÄ±n yolu. Nesnenin ekrandaki piksellerinin rengini hesaplar.
	 */
	private static final MyFile FRAGMENT_SHADER = new MyFile("entityRenderers",
			"entityFragment.txt");

	/**
	 * KameranÄ±n ve dÃ¼nyanÄ±n konumunu hesaplamak iÃ§in kullanÄ±lan matris deÄŸiÅŸkeni.
	 */
	protected UniformMatrix projectionViewMatrix = new UniformMatrix("projectionViewMatrix");
	
	/**
	 * Modelde ekstra bir kaplama haritasÄ± olup olmadÄ±ÄŸÄ±nÄ± belirten boolean (mantÄ±ksal) deÄŸiÅŸken.
	 */
	protected UniformBoolean hasExtraMap = new UniformBoolean("hasExtraMap");
	protected UniformVec4 baseColorFactor = new UniformVec4("baseColorFactor");
	protected UniformBoolean useFakeLighting = new UniformBoolean("useFakeLighting");
	
	/**
	 * Nesnenin dÃ¼nyadaki konumunu (translation) hesaplamak iÃ§in matris.
	 */
	protected UniformMatrix transformationMatrix = new UniformMatrix("transformationMatrix");
	
	/**
	 * IÅŸÄ±ÄŸÄ±n (Ã¶rneÄŸin gÃ¼neÅŸin) geliÅŸ yÃ¶nÃ¼nÃ¼ tutan 3 boyutlu vektÃ¶r deÄŸiÅŸkeni.
	 */
	protected UniformVec3 lightDirection = new UniformVec3("lightDirection");
	protected UniformVec3 lightColor = new UniformVec3("lightColor");
	protected UniformFloat lightBrightness = new UniformFloat("lightBrightness");
	protected UniformFloat ambientLight = new UniformFloat("ambientLight");
	
	/**
	 * KÄ±rpma dÃ¼zlemi vektÃ¶rÃ¼. Ã–rneÄŸin su altÄ± yansÄ±malarÄ±nda ekranÄ±n bir kÄ±smÄ±nÄ± kesmek iÃ§in kullanÄ±lÄ±r.
	 */
	protected UniformVec4 plane = new UniformVec4("plane");

	// Doku Atlas (Texture Atlas) UniformlarÄ±
	protected UniformFloat numberOfRows = new UniformFloat("numberOfRows");
	protected shaders.UniformVec2 textureOffset = new shaders.UniformVec2("textureOffset");

	// Ã‡oklu Nokta IÅŸÄ±k (Multiple Point Lights) UniformlarÄ± - MAX_LIGHTS = 4
	protected UniformVec3[] pointLightPos = new UniformVec3[4];
	protected UniformVec3[] pointLightColor = new UniformVec3[4];
	protected UniformVec3[] pointLightAttenuation = new UniformVec3[4];

	/**
	 * Modelin ana kaplamasÄ±nÄ± (resmini) tutan sampler.
	 */
	private UniformSampler diffuseMap = new UniformSampler("diffuseMap");
	
	/**
	 * Modelin ekstra kaplamasÄ±nÄ± (Ã¶rneÄŸin parlayan alanlar) tutan sampler.
	 */
	private UniformSampler extraMap = new UniformSampler("extraMap");

	protected UniformVec3 cameraPosition = new UniformVec3("cameraPosition");
	protected UniformMatrix toShadowMapSpace = new UniformMatrix("toShadowMapSpace");
	private UniformSampler shadowMap = new UniformSampler("shadowMap");

	// Cloud Shadows
	protected UniformFloat uCloudShadowEnabled = new UniformFloat("uCloudShadowEnabled");
	protected UniformVec4[] uCloudShadowPos = new UniformVec4[32];
	protected UniformFloat[] uCloudShadowAlpha = new UniformFloat[32];
	protected shaders.UniformInt uNumCloudShadows = new shaders.UniformInt("uNumCloudShadows");
	protected UniformFloat uTime = new UniformFloat("uTime");
	protected shaders.UniformVec2 uWindDir = new shaders.UniformVec2("uWindDir");

	// Skeletal Animation (Skinning) Uniforms
	private static final int MAX_JOINTS = 50;
	protected UniformMatrix[] jointTransforms = new UniformMatrix[MAX_JOINTS];

	/**
	 * Shader programÄ±nÄ± baÅŸlatan ve deÄŸiÅŸkenleri baÄŸlayan yapÄ±cÄ± (constructor) metot.
	 */
	public EntityShader() {
		super(VERTEX_SHADER, FRAGMENT_SHADER, "in_position", "in_textureCoords", "in_normal", "in_jointIndices", "in_weights");
		
		for (int i = 0; i < MAX_JOINTS; i++) {
			jointTransforms[i] = new UniformMatrix("jointTransforms[" + i + "]");
		}

		for (int i = 0; i < 4; i++) {
			pointLightPos[i] = new UniformVec3("pointLightPos[" + i + "]");
			pointLightColor[i] = new UniformVec3("pointLightColor[" + i + "]");
			pointLightAttenuation[i] = new UniformVec3("pointLightAttenuation[" + i + "]");
		}
		
		for (int i = 0; i < 32; i++) {
			uCloudShadowPos[i] = new UniformVec4("uCloudShadowPos[" + i + "]");
			uCloudShadowAlpha[i] = new UniformFloat("uCloudShadowAlpha[" + i + "]");
		}

		java.util.List<shaders.Uniform> list = new java.util.ArrayList<>();
		list.add(projectionViewMatrix);
		list.add(transformationMatrix);
		list.add(diffuseMap);
		list.add(extraMap);
		list.add(hasExtraMap);
		list.add(baseColorFactor);
		list.add(useFakeLighting);
		list.add(lightDirection);
		list.add(lightColor);
		list.add(lightBrightness);
		list.add(ambientLight);
		list.add(plane);
		list.add(numberOfRows);
		list.add(textureOffset);
		
		for (int i = 0; i < 4; i++) {
			list.add(pointLightPos[i]);
			list.add(pointLightColor[i]);
			list.add(pointLightAttenuation[i]);
		}
		list.add(cameraPosition);
		list.add(toShadowMapSpace);
		list.add(shadowMap);
		list.add(uCloudShadowEnabled);
		for (int i = 0; i < 32; i++) {
			list.add(uCloudShadowPos[i]);
			list.add(uCloudShadowAlpha[i]);
		}
		list.add(uNumCloudShadows);
		list.add(uTime);
		list.add(uWindDir);

		for (int i = 0; i < MAX_JOINTS; i++) {
			list.add(jointTransforms[i]);
		}

		super.storeAllUniformLocations(list.toArray(new shaders.Uniform[0]));
		connectTextureUnits();
	}

	public void loadCloudShadowData(float time, org.lwjgl.util.vector.Vector2f windDir, boolean enabled, java.util.List<skybox.atmosphere.CloudCluster> clusters) {
		uTime.loadFloat(time);
		uWindDir.loadVec2(windDir);
		uCloudShadowEnabled.loadFloat(enabled ? 1.0f : 0.0f);

		int count = 0;
		if (clusters != null) {
			count = Math.min(clusters.size(), 32);
		}
		uNumCloudShadows.loadInt(count);
		for (int i = 0; i < count; i++) {
			skybox.atmosphere.CloudCluster c = clusters.get(i);
			float r = c.currentScale * 650.0f;

			uCloudShadowPos[i].loadVec4(new org.lwjgl.util.vector.Vector4f(c.position.x, c.position.y, c.position.z, r));
			uCloudShadowAlpha[i].loadFloat(c.alpha);
		}
		for (int i = count; i < 32; i++) {
			uCloudShadowPos[i].loadVec4(new org.lwjgl.util.vector.Vector4f(0, 0, 0, 0));
			uCloudShadowAlpha[i].loadFloat(0.0f);
		}
	}

	/**
	 * Texture (kaplama) Ã¼nitelerini shader Ã¼zerindeki deÄŸiÅŸkenlere baÄŸlar.
	 * Neden: GLSL iÃ§indeki deÄŸiÅŸkenlerin hangi kaplamayÄ± (0 veya 1 numaralÄ±) kullanacaÄŸÄ±nÄ± belirtmek iÃ§indir.
	 */
	private void connectTextureUnits() {
		super.start();
		diffuseMap.loadTexUnit(0);
		extraMap.loadTexUnit(1);
		shadowMap.loadTexUnit(6);
		super.stop();
	}
}

