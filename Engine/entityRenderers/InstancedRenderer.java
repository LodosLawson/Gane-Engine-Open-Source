package entityRenderers;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Map;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

import openglObjects.Vao;
import openglObjects.Vbo;
import scene.Light;
import scene.Model;
import scene.Skin;

public class InstancedRenderer {

	private static final int MAX_INSTANCES = 500000;
	private static final int INSTANCE_DATA_LENGTH = 17;
	private static final FloatBuffer buffer = BufferUtils.createFloatBuffer(MAX_INSTANCES * INSTANCE_DATA_LENGTH);
	private static final float[] tempArray = new float[MAX_INSTANCES * INSTANCE_DATA_LENGTH];

	private InstancedShader shader;
	private Vbo vbo;
	private utils.Frustum frustum;

	public InstancedRenderer(InstancedShader shader) {
		this.shader = shader;
		this.frustum = new utils.Frustum();
		
		this.vbo = Vbo.create(GL15.GL_ARRAY_BUFFER);
		this.vbo.bind();
		this.vbo.allocateData(MAX_INSTANCES * INSTANCE_DATA_LENGTH);
		this.vbo.unbind();
	}

	public void render(Map<Model, Map<Skin, List<scene.InstanceData>>> instances, utils.ICamera camera, scene.Scene scene, Vector4f clipPlane, Matrix4f toShadowSpace) {
		shader.start();
		
		if (toShadowSpace != null) {
			shader.toShadowMapSpace.loadMatrix(toShadowSpace);
		}
		
		Matrix4f projectionViewMatrix = camera.getProjectionViewMatrix();
		shader.projectionViewMatrix.loadMatrix(projectionViewMatrix);
		frustum.update(projectionViewMatrix);
		
		shader.plane.loadVec4(clipPlane);
		
		shader.lightDirection.loadVec3(scene.getLightDirection());
		shader.lightColor.loadVec3(scene.getLightColor());
		shader.lightBrightness.loadFloat(scene.getLightBrightness());
		shader.ambientLight.loadFloat(scene.getAmbientLight());
		
		shader.uFogColor.loadVec3(scene.getFogColor());
		shader.uFogDensity.loadFloat(scene.getFogDensity());
		shader.uFogStart.loadFloat(scene.getFogStart());
		
		scene.Light pointLight = scene.getPointLight();
		if (pointLight != null) {
			shader.pointLightPos.loadVec3(pointLight.getPosition());
			shader.pointLightColor.loadVec3(pointLight.getColor());
			shader.pointLightAttenuation.loadVec3(pointLight.getAttenuation());
		} else {
			shader.pointLightColor.loadVec3(new org.lwjgl.util.vector.Vector3f(0, 0, 0));
			shader.pointLightAttenuation.loadVec3(new org.lwjgl.util.vector.Vector3f(1, 0, 0));
		}
		shader.cameraPosition.loadVec3(camera.getPosition());

		if (scene.getSky() != null && scene.getSky() instanceof skybox.atmosphere.AtmosphereSky) {
			skybox.atmosphere.AtmosphereSky sky = (skybox.atmosphere.AtmosphereSky) scene.getSky();
			shader.loadCloudShadowData(sky.getTime(), new org.lwjgl.util.vector.Vector2f(scene.getWindVelocity().x, scene.getWindVelocity().z), sky.isCloudsEnabled(), sky.getClusters());
		} else {
			shader.loadCloudShadowData(0f, new org.lwjgl.util.vector.Vector2f(0, 0), false, null);
		}

		org.lwjgl.util.vector.Vector3f camPos = camera.getPosition();

		for (Model model : instances.keySet()) {
			Vao vao = model.getVao();
			
			vbo.bind();
			vao.addInstancedAttribute(vao.id, vbo.getId(), 3, 4, INSTANCE_DATA_LENGTH, 0);
			vao.addInstancedAttribute(vao.id, vbo.getId(), 4, 4, INSTANCE_DATA_LENGTH, 4);
			vao.addInstancedAttribute(vao.id, vbo.getId(), 5, 4, INSTANCE_DATA_LENGTH, 8);
			vao.addInstancedAttribute(vao.id, vbo.getId(), 6, 4, INSTANCE_DATA_LENGTH, 12);
			vao.addInstancedAttribute(vao.id, vbo.getId(), 7, 1, INSTANCE_DATA_LENGTH, 16);
			vbo.unbind();
			
			vao.bind(0, 1, 2);
			
			GL20.glEnableVertexAttribArray(3);
			GL20.glEnableVertexAttribArray(4);
			GL20.glEnableVertexAttribArray(5);
			GL20.glEnableVertexAttribArray(6);
			GL20.glEnableVertexAttribArray(7);

				boolean isGrass = false;
				boolean isTree = false;
				try {
					isGrass = (model == objects.Grass3D.getGrassModel());
					isTree = (model == objects.Tree3D.getTreeModel());
				} catch (Throwable t) {}
				
				// OPTIMIZATION: Do not render grass in water reflection passes!
				// Grass reflection is barely visible, but costs massive GPU fill-rate.
				if (isGrass && clipPlane.y != 0.0f) {
					continue;
				}
				
				float maxDistanceSq = 1200.0f * 1200.0f;
				if (isGrass) maxDistanceSq = 350.0f * 350.0f;
				else if (isTree) maxDistanceSq = 1000.0f * 1000.0f;

			Map<Skin, List<scene.InstanceData>> skinMap = instances.get(model);
			for (Skin skin : skinMap.keySet()) {
				prepareSkin(skin);
				
				List<scene.InstanceData> allData = skinMap.get(skin);
				buffer.clear();
				int count = 0;
				int index = 0;
				
				for(scene.InstanceData data : allData) {
					Matrix4f mat = data.getTransform();
					float dx = mat.m30 - camPos.x;
					float dy = mat.m31 - camPos.y;
					float dz = mat.m32 - camPos.z;
					float distSq = dx * dx + dy * dy + dz * dz;
					if (distSq > maxDistanceSq) {
						continue;
					}
					
					// Optimizasyon: FloraManager zaten chunk (bölge) bazında frustum culling yaptığı için
					// her bir çimen/ağaç tanesi için 6 düzlemli frustum check yapmak CPU'yu kilitliyordu.
					// Bu O(N) yükü kaldırıldı! (Sadece basit mesafe kontrolü bırakıldı)
					
					if (count >= MAX_INSTANCES) {
						break;
					}
					tempArray[index++] = mat.m00;
						tempArray[index++] = mat.m01;
						tempArray[index++] = mat.m02;
						tempArray[index++] = mat.m03;
						tempArray[index++] = mat.m10;
						tempArray[index++] = mat.m11;
						tempArray[index++] = mat.m12;
						tempArray[index++] = mat.m13;
						tempArray[index++] = mat.m20;
						tempArray[index++] = mat.m21;
						tempArray[index++] = mat.m22;
						tempArray[index++] = mat.m23;
						tempArray[index++] = mat.m30;
						tempArray[index++] = mat.m31;
						tempArray[index++] = mat.m32;
					tempArray[index++] = mat.m33;
					tempArray[index++] = data.getTextureOffsetIndex();
					count++;
				}
				
				if(count == 0) continue;
				
				// Toplu olarak (bulk) NIO buffer'Ä±na kopyala (Ã‡ok daha hÄ±zlÄ±!)
				buffer.put(tempArray, 0, count * INSTANCE_DATA_LENGTH);
				buffer.flip();
				
				vbo.bind();
				vbo.updateData(buffer);
				vao.bindIndexBuffer();
				GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, vao.getIndexCount(), GL11.GL_UNSIGNED_INT, 0, count);
			}
			
			GL20.glDisableVertexAttribArray(3);
			GL20.glDisableVertexAttribArray(4);
			GL20.glDisableVertexAttribArray(5);
			GL20.glDisableVertexAttribArray(6);
			GL20.glDisableVertexAttribArray(7);
			
			vbo.unbind();
			vao.unbind(0, 1, 2);
		}
		
		shader.stop();
	}

	private void prepareSkin(Skin skin) {
		if (skin.isCullBackFaces()) {
			GL11.glEnable(GL11.GL_CULL_FACE);
			GL11.glCullFace(GL11.GL_BACK);
		} else {
			GL11.glDisable(GL11.GL_CULL_FACE);
		}

		skin.getDiffuseTexture().bindToUnit(0);
		
		if (skin.hasExtraMap()) {
			shader.hasExtraMap.loadBoolean(true);
			skin.getExtraInfoMap().bindToUnit(1);
		} else {
			shader.hasExtraMap.loadBoolean(false);
		}
		
		shader.useFakeLighting.loadBoolean(skin.isUseFakeLighting());
		shader.numberOfRows.loadFloat((float) skin.getNumberOfRows());
	}
	
	public void cleanUp() {
		shader.cleanUp();
		vbo.delete();
	}
}

