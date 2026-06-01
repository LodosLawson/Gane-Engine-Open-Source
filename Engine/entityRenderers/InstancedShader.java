package entityRenderers;

import shaders.ShaderProgram;
import shaders.UniformBoolean;
import shaders.UniformFloat;
import shaders.UniformMatrix;
import shaders.UniformSampler;
import shaders.UniformVec3;
import shaders.UniformVec4;
import utils.MyFile;

public class InstancedShader extends ShaderProgram {

	private static final MyFile VERTEX_SHADER = new MyFile("entityRenderers", "instancedVertex.txt");
	private static final MyFile FRAGMENT_SHADER = new MyFile("entityRenderers", "instancedFragment.txt");

	protected UniformMatrix projectionViewMatrix = new UniformMatrix("projectionViewMatrix");
	
	protected UniformBoolean hasExtraMap = new UniformBoolean("hasExtraMap");
	protected UniformBoolean useFakeLighting = new UniformBoolean("useFakeLighting");
	protected UniformFloat numberOfRows = new UniformFloat("numberOfRows");
	
	protected UniformVec3 lightDirection = new UniformVec3("lightDirection");
	protected UniformVec3 lightColor = new UniformVec3("lightColor");
	protected UniformFloat lightBrightness = new UniformFloat("lightBrightness");
	protected UniformFloat ambientLight = new UniformFloat("ambientLight");
	
	protected UniformVec4 plane = new UniformVec4("plane");

	protected UniformVec3 pointLightPos = new UniformVec3("pointLightPos");
	protected UniformVec3 pointLightColor = new UniformVec3("pointLightColor");
	protected UniformVec3 pointLightAttenuation = new UniformVec3("pointLightAttenuation");
	protected UniformVec3 cameraPosition = new UniformVec3("cameraPosition");

	protected UniformMatrix toShadowMapSpace = new UniformMatrix("toShadowMapSpace");

	private UniformSampler diffuseMap = new UniformSampler("diffuseMap");
	private UniformSampler extraMap = new UniformSampler("extraMap");
	private UniformSampler shadowMap = new UniformSampler("shadowMap");

	// Cloud Shadows
	protected UniformFloat uCloudShadowEnabled = new UniformFloat("uCloudShadowEnabled");
	protected UniformVec4[] uCloudShadowPos = new UniformVec4[32];
	protected UniformFloat[] uCloudShadowAlpha = new UniformFloat[32];
	protected shaders.UniformInt uNumCloudShadows = new shaders.UniformInt("uNumCloudShadows");
	protected UniformFloat uTime = new UniformFloat("uTime");
	protected shaders.UniformVec2 uWindDir = new shaders.UniformVec2("uWindDir");

	public InstancedShader() {
		super(VERTEX_SHADER, FRAGMENT_SHADER, "in_position", "in_textureCoords", "in_normal", "transformationMatrix");
		
		for (int i = 0; i < 32; i++) {
			uCloudShadowPos[i] = new UniformVec4("uCloudShadowPos[" + i + "]");
			uCloudShadowAlpha[i] = new UniformFloat("uCloudShadowAlpha[" + i + "]");
		}

		java.util.List<shaders.Uniform> list = new java.util.ArrayList<>();
		list.add(projectionViewMatrix);
		list.add(diffuseMap);
		list.add(extraMap);
		list.add(hasExtraMap);
		list.add(lightDirection);
		list.add(lightColor);
		list.add(lightBrightness);
		list.add(ambientLight);
		list.add(plane);
		list.add(useFakeLighting);
		list.add(numberOfRows);
		list.add(pointLightPos);
		list.add(pointLightColor);
		list.add(pointLightAttenuation);
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

	private void connectTextureUnits() {
		super.start();
		diffuseMap.loadTexUnit(0);
		extraMap.loadTexUnit(1);
		shadowMap.loadTexUnit(6); // Bind to TEXTURE6, same as terrain
		super.stop();
	}
}

