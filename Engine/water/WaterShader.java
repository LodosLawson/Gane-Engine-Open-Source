package water;

import shaders.ShaderProgram;
import shaders.UniformFloat;
import shaders.UniformMatrix;
import shaders.UniformSampler;
import shaders.UniformVec3;
import utils.MyFile;

/**
 * Gelişmiş su gölgelendirici (Shader) sınıfı.
 * 3D dalga, dinamik renk, saydamlık, fresnel ve specular parametreleri
 * WaterTile'dan runtime'da yüklenir.
 */
public class WaterShader extends ShaderProgram {

	private static final MyFile VERTEX_SHADER = new MyFile("water", "waterVertex.txt");
	private static final MyFile FRAGMENT_SHADER = new MyFile("water", "waterFragment.txt");

	// Dönüşüm Matrisleri
	protected UniformMatrix modelMatrix = new UniformMatrix("modelMatrix");
	protected UniformMatrix viewMatrix = new UniformMatrix("viewMatrix");
	protected UniformMatrix projectionMatrix = new UniformMatrix("projectionMatrix");

	// Dalga parametreleri
	protected UniformFloat moveFactor = new UniformFloat("moveFactor");
	protected UniformFloat waveStrength = new UniformFloat("waveStrength");
	protected UniformFloat waveAmplitude = new UniformFloat("waveAmplitude");
	protected UniformFloat waveFrequency = new UniformFloat("waveFrequency");

	// Renk ve saydamlık
	protected UniformVec3 waterColour = new UniformVec3("waterColour");
	protected UniformFloat transparency = new UniformFloat("transparency");
	protected UniformFloat colorMixFactor = new UniformFloat("colorMixFactor");

	// Fresnel & Specular
	protected UniformFloat shineDamper = new UniformFloat("shineDamper");
	protected UniformFloat reflectivity = new UniformFloat("reflectivity");
	protected UniformFloat fresnelPower = new UniformFloat("fresnelPower");

	// Kamera ve Işık
	protected UniformVec3 cameraPosition = new UniformVec3("cameraPosition");
	protected UniformVec3 lightDirection = new UniformVec3("lightDirection");

	// Doku (Texture) sampler'ları
	private UniformSampler reflectionTexture = new UniformSampler("reflectionTexture");
	private UniformSampler refractionTexture = new UniformSampler("refractionTexture");
	private UniformSampler dudvMap = new UniformSampler("dudvMap");
	private UniformSampler normalMap = new UniformSampler("normalMap");
	private UniformSampler depthMap = new UniformSampler("depthMap");

	public WaterShader() {
		super(VERTEX_SHADER, FRAGMENT_SHADER, "position");
		super.storeAllUniformLocations(
			modelMatrix, viewMatrix, projectionMatrix,
			moveFactor, waveStrength, waveAmplitude, waveFrequency,
			waterColour, transparency, colorMixFactor,
			shineDamper, reflectivity, fresnelPower,
			cameraPosition, lightDirection,
			reflectionTexture, refractionTexture, dudvMap, normalMap, depthMap
		);
		connectTextureUnits();
	}

	private void connectTextureUnits() {
		super.start();
		reflectionTexture.loadTexUnit(0);
		refractionTexture.loadTexUnit(1);
		dudvMap.loadTexUnit(2);
		normalMap.loadTexUnit(3);
		depthMap.loadTexUnit(4);
		super.stop();
	}

}
