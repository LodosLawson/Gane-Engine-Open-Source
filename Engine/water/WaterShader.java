package water;

import shaders.ShaderProgram;
import shaders.UniformFloat;
import shaders.UniformMatrix;
import shaders.UniformSampler;
import shaders.UniformVec3;
import utils.MyFile;

/**
 * Suyun renderlanmasında kullanılan özel gölgelendirici (Shader) sınıfı.
 * Suyun hareketi, yansıma, kırılma ve fresnel efektlerini hesaplar.
 */
public class WaterShader extends ShaderProgram {

	private static final MyFile VERTEX_SHADER = new MyFile("water", "waterVertex.txt");
	private static final MyFile FRAGMENT_SHADER = new MyFile("water", "waterFragment.txt");

	// Dönüşüm Matrisleri
	protected UniformMatrix modelMatrix = new UniformMatrix("modelMatrix");
	protected UniformMatrix viewMatrix = new UniformMatrix("viewMatrix");
	protected UniformMatrix projectionMatrix = new UniformMatrix("projectionMatrix");

	// Suyun dalgalanması için hareket çarpanı
	protected UniformFloat moveFactor = new UniformFloat("moveFactor");
	// Fresnel etkisi (Görüş açısına göre yansıma oranı) için kamera konumu
	protected UniformVec3 cameraPosition = new UniformVec3("cameraPosition");
	// Suyun üzerindeki specular yansımaları için ışık yönü
	protected UniformVec3 lightDirection = new UniformVec3("lightDirection");

	// Kullanılan Dokular (Textures)
	private UniformSampler reflectionTexture = new UniformSampler("reflectionTexture");
	private UniformSampler refractionTexture = new UniformSampler("refractionTexture");
	private UniformSampler dudvMap = new UniformSampler("dudvMap");
	private UniformSampler normalMap = new UniformSampler("normalMap");
	private UniformSampler depthMap = new UniformSampler("depthMap"); // Suyun kıyılarını yumuşatmak için

	/**
	 * Shader dosyasını derler ve Uniform değişkenlerinin yerlerini tespit eder.
	 */
	public WaterShader() {
		super(VERTEX_SHADER, FRAGMENT_SHADER, "position");
		super.storeAllUniformLocations(modelMatrix, viewMatrix, projectionMatrix, moveFactor,
				cameraPosition, lightDirection, reflectionTexture, refractionTexture,
				dudvMap, normalMap, depthMap);
		connectTextureUnits();
	}

	/**
	 * Shader içindeki texture sampler'larını OpenGL Doku Üniteleri (Texture Units) ile eşleştirir.
	 */
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
