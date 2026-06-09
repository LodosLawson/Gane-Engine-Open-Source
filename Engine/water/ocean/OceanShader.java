package water.ocean;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import shaders.ShaderProgram;
import shaders.UniformFloat;
import shaders.UniformMatrix;
import shaders.UniformVec2;
import shaders.UniformVec3;
import shaders.UniformVec4;
import utils.MyFile;
import utils.ICamera;

public class OceanShader extends ShaderProgram {

	private static final MyFile VERTEX_FILE = new MyFile("water/ocean/ocean.vert");
	private static final MyFile FRAGMENT_FILE = new MyFile("water/ocean/ocean.frag");

	private UniformMatrix matLocal = new UniformMatrix("matLocal");
	private UniformMatrix matWorld = new UniformMatrix("matWorld");
	private UniformMatrix matViewProj = new UniformMatrix("matViewProj");
	
	private UniformVec4 uvParams = new UniformVec4("uvParams");
	private UniformVec2 perlinOffset = new UniformVec2("perlinOffset");
	private UniformVec3 eyePos = new UniformVec3("eyePos");
	private UniformVec3 oceanColor = new UniformVec3("oceanColor");

	// Scene Lighting
	protected UniformVec3 lightDirection = new UniformVec3("lightDirection");
	protected UniformVec3 lightColor = new UniformVec3("lightColor");
	protected UniformVec3 lightAmbient = new UniformVec3("lightAmbient");

	// Cloud Shadows
	private UniformFloat uTime = new UniformFloat("uTime");
	private UniformFloat uCloudShadowEnabled = new UniformFloat("uCloudShadowEnabled");
	private UniformVec4[] uCloudShadowPos = new UniformVec4[32];
	private UniformFloat[] uCloudShadowAlpha = new UniformFloat[32];
	private shaders.UniformInt uNumCloudShadows = new shaders.UniformInt("uNumCloudShadows");

	// Fog params
	private UniformVec3 uFogColor = new UniformVec3("uFogColor");
	private UniformFloat uFogDensity = new UniformFloat("uFogDensity");
	private UniformFloat uFogStart = new UniformFloat("uFogStart");
	
	private shaders.UniformFloat uWindStrength = new shaders.UniformFloat("uWindStrength");

	// Gemi su alma önlemi (İçeri giren suyu kesmek için)
	private UniformVec3 uShipPos = new UniformVec3("uShipPos");
	private UniformVec2 uShipDim = new UniformVec2("uShipDim"); // x=Uzunluk, y=Genişlik
	private UniformFloat uShipYaw = new UniformFloat("uShipYaw");
	private UniformFloat uShipEnabled = new UniformFloat("uShipEnabled");

	// Depth and Shore blending
	private shaders.UniformFloat uNearPlane = new shaders.UniformFloat("uNearPlane");
	private shaders.UniformFloat uFarPlane = new shaders.UniformFloat("uFarPlane");

	public OceanShader() {
		super(new MyFile("water/ocean", "ocean.vert"), new MyFile("water/ocean", "ocean.frag"), "my_Position");
		
		for (int i = 0; i < 32; i++) {
			uCloudShadowPos[i] = new UniformVec4("uCloudShadowPos[" + i + "]");
			uCloudShadowAlpha[i] = new UniformFloat("uCloudShadowAlpha[" + i + "]");
		}

		java.util.List<shaders.Uniform> list = new java.util.ArrayList<>();
		list.add(matLocal);
		list.add(matWorld);
		list.add(matViewProj);
		list.add(uvParams);
		list.add(perlinOffset);
		list.add(oceanColor);
		list.add(eyePos);
		list.add(lightDirection);
		list.add(lightColor);
		list.add(lightAmbient);
		list.add(uTime);
		list.add(uCloudShadowEnabled);
		
		for (int i = 0; i < 32; i++) {
			list.add(uCloudShadowPos[i]);
			list.add(uCloudShadowAlpha[i]);
		}
		list.add(uNumCloudShadows);
		list.add(uFogColor);
		list.add(uFogDensity);
		list.add(uFogStart);
		list.add(uWindStrength);
		list.add(uNearPlane);
		list.add(uFarPlane);
		
		list.add(uShipPos);
		list.add(uShipDim);
		list.add(uShipYaw);
		list.add(uShipEnabled);

		super.storeAllUniformLocations(list.toArray(new shaders.Uniform[0]));
	}

	public void loadShipCutout(Vector3f pos, Vector2f dim, float yaw, boolean enabled) {
		uShipPos.loadVec3(pos);
		uShipDim.loadVec2(dim);
		uShipYaw.loadFloat(yaw);
		uShipEnabled.loadFloat(enabled ? 1.0f : 0.0f);
	}

	public void loadMatLocal(Matrix4f matrix) {
		matLocal.loadMatrix(matrix);
	}

	public void loadMatWorld(Matrix4f matrix) {
		matWorld.loadMatrix(matrix);
	}

	public void loadMatViewProj(ICamera camera) {
		matViewProj.loadMatrix(camera.getProjectionViewMatrix());
		eyePos.loadVec3(camera.getPosition());
	}


	public void loadUVParams(Vector4f params) {
		uvParams.loadVec4(params);
	}

	public void loadPerlinOffset(Vector2f offset) {
		perlinOffset.loadVec2(offset);
	}

	public void loadOceanColor(Vector3f color) {
		oceanColor.loadVec3(color);
	}

	public void loadLighting(Vector3f lightDir, Vector3f color, Vector3f ambient) {
		lightDirection.loadVec3(lightDir);
		lightColor.loadVec3(color);
		lightAmbient.loadVec3(ambient);
	}

	public void loadFogParams(Vector3f color, float density, float start) {
		uFogColor.loadVec3(color);
		uFogDensity.loadFloat(density);
		uFogStart.loadFloat(start);
	}

	public void loadCloudShadowData(float time, org.lwjgl.util.vector.Vector2f windDir, boolean enabled, java.util.List<skybox.atmosphere.CloudCluster> clusters) {
		uTime.loadFloat(time);
		uCloudShadowEnabled.loadFloat(enabled ? 1.0f : 0.0f);

		int count = 0;
		if (clusters != null) {
			count = Math.min(clusters.size(), 32);
		}
		uNumCloudShadows.loadInt(count);
		for (int i = 0; i < count; i++) {
			skybox.atmosphere.CloudCluster c = clusters.get(i);
			float r = c.currentScale * 450.0f;
			uCloudShadowPos[i].loadVec4(new org.lwjgl.util.vector.Vector4f(c.position.x, c.position.y, c.position.z, r));
			uCloudShadowAlpha[i].loadFloat(c.alpha);
		}
		for (int i = count; i < 32; i++) {
			uCloudShadowPos[i].loadVec4(new org.lwjgl.util.vector.Vector4f(0, 0, 0, 0));
			uCloudShadowAlpha[i].loadFloat(0.0f);
		}
	}
	
	public void loadWindStrength(float strength) {
		uWindStrength.loadFloat(strength);
	}

	public void loadDepthParams(float near, float far) {
		uNearPlane.loadFloat(near);
		uFarPlane.loadFloat(far);
	}
}

