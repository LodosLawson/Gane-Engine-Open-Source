package water.tile;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import shaders.ShaderProgram;
import shaders.UniformFloat;
import shaders.UniformMatrix;
import shaders.UniformSampler;
import shaders.UniformVec3;
import utils.ICamera;
import utils.MyFile;

public class WaterShader extends ShaderProgram {

	private static final MyFile VERTEX_FILE = new MyFile("water/tile", "waterVertex.txt");
	private static final MyFile FRAGMENT_FILE = new MyFile("water/tile", "waterFragment.txt");

	// Matrices
	protected UniformMatrix modelMatrix = new UniformMatrix("modelMatrix");
	protected UniformMatrix viewMatrix = new UniformMatrix("viewMatrix");
	protected UniformMatrix projectionMatrix = new UniformMatrix("projectionMatrix");
	
	// Textures
	private UniformSampler reflectionTexture = new UniformSampler("reflectionTexture");
	private UniformSampler refractionTexture = new UniformSampler("refractionTexture");
	private UniformSampler normalMap = new UniformSampler("normalMap");
	private UniformSampler depthMap = new UniformSampler("depthMap");
	
	// Camera and Light
	protected UniformVec3 cameraPosition = new UniformVec3("cameraPosition");
	protected UniformVec3 lightPosition = new UniformVec3("lightPosition");
	protected UniformVec3 lightColor = new UniformVec3("lightColor");
	protected UniformVec3 lightAmbient = new UniformVec3("lightAmbient");
	
	// Water parameters
	protected UniformFloat time = new UniformFloat("time");
	protected UniformVec3 baseColor = new UniformVec3("baseColor");
	protected UniformFloat transparency = new UniformFloat("transparency");
	protected UniformFloat textureScale = new UniformFloat("textureScale");
	
	// Camera depth planes
	protected UniformFloat nearPlane = new UniformFloat("nearPlane");
	protected UniformFloat farPlane = new UniformFloat("farPlane");

	public WaterShader() {
		super(VERTEX_FILE, FRAGMENT_FILE, "position");
		super.storeAllUniformLocations(
			modelMatrix, viewMatrix, projectionMatrix,
			reflectionTexture, refractionTexture, normalMap, depthMap,
			cameraPosition, lightPosition, lightColor, lightAmbient,
			time, baseColor, transparency, textureScale, nearPlane, farPlane
		);
		connectTextureUnits();
	}

	public void connectTextureUnits() {
		super.start();
		reflectionTexture.loadTexUnit(0);
		refractionTexture.loadTexUnit(1);
		normalMap.loadTexUnit(2);
		depthMap.loadTexUnit(3);
		super.stop();
	}
	
	public void loadLighting(Vector3f lightDir, Vector3f color, Vector3f ambient) {
		// Directional light position is opposite to light direction
		lightPosition.loadVec3(new Vector3f(-lightDir.x * 1000f, -lightDir.y * 1000f, -lightDir.z * 1000f));
		lightColor.loadVec3(color);
		lightAmbient.loadVec3(ambient);
	}

	public void loadProjectionMatrix(Matrix4f projection) {
		projectionMatrix.loadMatrix(projection);
	}
	
	public void loadViewMatrix(ICamera camera){
		Matrix4f view = camera.getViewMatrix();
		viewMatrix.loadMatrix(view);
		cameraPosition.loadVec3(camera.getPosition());
	}

	public void loadModelMatrix(Matrix4f matrix){
		modelMatrix.loadMatrix(matrix);
	}
	
	public void loadWaterParameters(float timeVal, Vector3f color, float trans, float texScale) {
		time.loadFloat(timeVal);
		baseColor.loadVec3(color);
		transparency.loadFloat(trans);
		textureScale.loadFloat(texScale);
	}
	
	public void loadDepthPlanes(float near, float far) {
		nearPlane.loadFloat(near);
		farPlane.loadFloat(far);
	}
}


