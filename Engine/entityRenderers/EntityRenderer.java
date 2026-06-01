package entityRenderers;

import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import openglObjects.Vao;
import scene.Entity;
import scene.Skin;
import utils.ICamera;
import utils.OpenGlUtils;

/**
 * Bu sÄ±nÄ±f sahnedeki nesneleri (entity) ekrana Ã§izdirmek (render) iÃ§in kullanÄ±lÄ±r.
 * Oyun iÃ§indeki 3D modellerin gÃ¶rÃ¼ntÃ¼lenmesinden sorumludur.
 */
public class EntityRenderer {
	
	/**
	 * Nesnelerin Ã§iziminde kullanÄ±lan shader (gÃ¶lgelendirici) programÄ±.
	 * GÃ¶lgelendirme, renk ve Ä±ÅŸÄ±k hesaplamalarÄ± iÃ§in gereklidir.
	 */
	private EntityShader shader;
	private utils.Frustum frustum;
	private java.util.Map<scene.Model, java.util.Map<scene.Skin, java.util.List<Entity>>> batchMap = new java.util.HashMap<>();

	private java.util.List<Entity> occludedEntities = new java.util.ArrayList<>(128);
	private org.lwjgl.util.vector.Matrix4f transformMatrix = new org.lwjgl.util.vector.Matrix4f();
	private org.lwjgl.util.vector.Vector3f scaleVector = new org.lwjgl.util.vector.Vector3f();
	private org.lwjgl.util.vector.Vector2f textureOffsetVec = new org.lwjgl.util.vector.Vector2f();
	
	private static final org.lwjgl.util.vector.Vector3f AXIS_X = new org.lwjgl.util.vector.Vector3f(1, 0, 0);
	private static final org.lwjgl.util.vector.Vector3f AXIS_Y = new org.lwjgl.util.vector.Vector3f(0, 1, 0);
	private static final org.lwjgl.util.vector.Vector3f AXIS_Z = new org.lwjgl.util.vector.Vector3f(0, 0, 1);

	/**
	 * EntityRenderer sÄ±nÄ±fÄ±nÄ±n yapÄ±cÄ± (constructor) metodu.
	 * Shader objesini oluÅŸturur ve hazÄ±rlar.
	 */
	public EntityRenderer() {
		this.shader = new EntityShader();
		this.frustum = new utils.Frustum();
	}

	/**
	 * Listede bulunan tÃ¼m nesneleri (entities) ekrana Ã§izer.
	 * Neden: Oyun dÃ¶ngÃ¼sÃ¼nde her karede (frame) nesnelerin gÃ¶rÃ¼nÃ¼r olmasÄ±nÄ± saÄŸlamak iÃ§in Ã§aÄŸrÄ±lÄ±r.
	 * @param entities Ã‡izilecek nesnelerin listesi.
	 * @param camera Oyuncunun kamerasÄ±, bakÄ±ÅŸ aÃ§Ä±sÄ±.
	 * @param lightDir GÃ¼neÅŸ Ä±ÅŸÄ±ÄŸÄ±nÄ±n veya ana Ä±ÅŸÄ±ÄŸÄ±n yÃ¶nÃ¼.
	 * @param clipPlane KÄ±rpma dÃ¼zlemi, su yansÄ±masÄ± vb. iÃ§in ekranÄ±n belli bir kÄ±smÄ±nÄ± kÄ±rpmak iÃ§in kullanÄ±lÄ±r.
	 */
	public void render(List<Entity> entities, ICamera camera, scene.Scene scene, Vector4f clipPlane, org.lwjgl.util.vector.Matrix4f toShadowSpace) {
		prepare(camera, scene, clipPlane, toShadowSpace);
		
		if (scene.isFrustumCullingEnabled()) {
			frustum.update(camera.getProjectionViewMatrix());
		}
		
		// 1. Clear previous batch data without generating Garbage (GC)
		for (java.util.Map<scene.Skin, java.util.List<Entity>> skinMap : batchMap.values()) {
			for (java.util.List<Entity> list : skinMap.values()) {
				list.clear();
			}
		}
		
		// 2. Batch entities by Model and Skin
		for (Entity entity : entities) {
			if (entity.getModel() == null) continue;
			
			if (scene.isFrustumCullingEnabled()) {
				if (!frustum.isPointInside(entity.getPosition().x, entity.getPosition().y, entity.getPosition().z, entity.getCullingRadius())) {
					continue;
				}
			}
			
			scene.Model currentModel = entity.getModel();
			
			if (scene.isLodEnabled()) {
				float dx = entity.getPosition().x - camera.getPosition().x;
				float dy = entity.getPosition().y - camera.getPosition().y;
				float dz = entity.getPosition().z - camera.getPosition().z;
				float distanceSq = dx*dx + dy*dy + dz*dz;
				
				if (entity.getLod2Model() != null && distanceSq > (entity.getLod2Distance() * entity.getLod2Distance())) {
					currentModel = entity.getLod2Model();
				} else if (entity.getLod1Model() != null && distanceSq > (entity.getLod1Distance() * entity.getLod1Distance())) {
					currentModel = entity.getLod1Model();
				}
			}
			
			scene.Skin currentSkin = entity.getSkin();
			
			java.util.Map<scene.Skin, java.util.List<Entity>> skinMap = batchMap.get(currentModel);
			if (skinMap == null) {
				skinMap = new java.util.HashMap<>();
				batchMap.put(currentModel, skinMap);
			}
			java.util.List<Entity> list = skinMap.get(currentSkin);
			if (list == null) {
				list = new java.util.ArrayList<>(64);
				skinMap.put(currentSkin, list);
			}
			list.add(entity);
		}
		
		occludedEntities.clear();
		
		// 3. Render Batched Entities (Visible Pass)
		for (scene.Model model : batchMap.keySet()) {
			java.util.Map<scene.Skin, java.util.List<Entity>> skinMap = batchMap.get(model);
			if (skinMap.isEmpty()) continue;
			
			Vao vao = model.getVao();
			vao.bind(0, 1, 2);
			
			for (scene.Skin skin : skinMap.keySet()) {
				java.util.List<Entity> list = skinMap.get(skin);
				if (list.isEmpty()) continue;
				
				prepareSkin(skin);
				shader.numberOfRows.loadFloat((float) skin.getNumberOfRows());
				
				for (Entity entity : list) {
					// --- Occlusion Culling Test ---
					if (scene.isOcclusionCullingEnabled()) {
						openglObjects.Query q = entity.getOcclusionQuery();
						if (q == null) {
							q = new openglObjects.Query(org.lwjgl.opengl.GL15.GL_SAMPLES_PASSED);
							entity.setOcclusionQuery(q);
						}
						// EÃŸer sorgu sonucu hazÄ±rsa, objenin gÃ¶rÃ¼nÃ¼r olup olmadÄ±ÄŸÄ±nÄ± gÃ¼ncelle
						if (q.isResultReady()) {
							entity.setVisible(q.getResult() > 0);
						}
					}
					
					// EÄŸer obje saklanmÄ±ÅŸsa (arkada kalmÄ±ÅŸsa), gerÃ§ek Ã§izimi atla.
					if (scene.isOcclusionCullingEnabled() && !entity.isVisible()) {
						occludedEntities.add(entity);
						continue;
					}

					textureOffsetVec.set(entity.getTextureXOffset(), entity.getTextureYOffset());
					shader.textureOffset.loadVec2(textureOffsetVec);
					
					transformMatrix.setIdentity();
					org.lwjgl.util.vector.Matrix4f.translate(entity.getPosition(), transformMatrix, transformMatrix);
					
					if (entity.getRotation().x != 0) {
						org.lwjgl.util.vector.Matrix4f.rotate((float) Math.toRadians(entity.getRotation().x), AXIS_X, transformMatrix, transformMatrix);
					}
					if (entity.getRotation().y != 0) {
						org.lwjgl.util.vector.Matrix4f.rotate((float) Math.toRadians(entity.getRotation().y), AXIS_Y, transformMatrix, transformMatrix);
					}
					if (entity.getRotation().z != 0) {
						org.lwjgl.util.vector.Matrix4f.rotate((float) Math.toRadians(entity.getRotation().z), AXIS_Z, transformMatrix, transformMatrix);
					}
					if (entity.getScale() != 1.0f) {
						scaleVector.set(entity.getScale(), entity.getScale(), entity.getScale());
						org.lwjgl.util.vector.Matrix4f.scale(scaleVector, transformMatrix, transformMatrix);
					}
					
					shader.transformationMatrix.loadMatrix(transformMatrix);
					
					if (scene.isOcclusionCullingEnabled()) entity.getOcclusionQuery().start();
					GL11.glDrawElements(GL11.GL_TRIANGLES, vao.getIndexCount(), GL11.GL_UNSIGNED_INT, 0);
					if (scene.isOcclusionCullingEnabled()) entity.getOcclusionQuery().end();
				}
			}
			vao.unbind(0, 1, 2);
		}
		
		// 4. Render Occluded Entities (Test Pass)
		// Saklanan objelerin tekrar gÃ¶rÃ¼nÃ¼r olup olmadÄ±ÄŸÄ±nÄ± anlamak iÃ§in renk ve derinlik yazmasÄ±nÄ± kapatarak "hayalet" Ã§izim yapÄ±yoruz.
		if (scene.isOcclusionCullingEnabled() && !occludedEntities.isEmpty()) {
			GL11.glColorMask(false, false, false, false); // Renk yazmayÄ± kapat
			GL11.glDepthMask(false); // Derinlik yazmayÄ± kapat
			
			for (Entity entity : occludedEntities) {
				Vao vao = entity.getModel().getVao();
				vao.bind(0, 1, 2);
				
				transformMatrix.setIdentity();
				org.lwjgl.util.vector.Matrix4f.translate(entity.getPosition(), transformMatrix, transformMatrix);
				if (entity.getRotation().x != 0) org.lwjgl.util.vector.Matrix4f.rotate((float) Math.toRadians(entity.getRotation().x), AXIS_X, transformMatrix, transformMatrix);
				if (entity.getRotation().y != 0) org.lwjgl.util.vector.Matrix4f.rotate((float) Math.toRadians(entity.getRotation().y), AXIS_Y, transformMatrix, transformMatrix);
				if (entity.getRotation().z != 0) org.lwjgl.util.vector.Matrix4f.rotate((float) Math.toRadians(entity.getRotation().z), AXIS_Z, transformMatrix, transformMatrix);
				if (entity.getScale() != 1.0f) {
					scaleVector.set(entity.getScale(), entity.getScale(), entity.getScale());
					org.lwjgl.util.vector.Matrix4f.scale(scaleVector, transformMatrix, transformMatrix);
				}
				
				shader.transformationMatrix.loadMatrix(transformMatrix);
				
				entity.getOcclusionQuery().start();
				GL11.glDrawElements(GL11.GL_TRIANGLES, vao.getIndexCount(), GL11.GL_UNSIGNED_INT, 0);
				entity.getOcclusionQuery().end();
				
				vao.unbind(0, 1, 2);
			}
			
			GL11.glColorMask(true, true, true, true); // Normale dÃ¶n
			GL11.glDepthMask(true); // Normale dÃ¶n
		}
		
		finish();
	}
	
	/**
	 * Uygulama kapanÄ±rken veya sÄ±nÄ±f yok edilirken shader kaynaklarÄ±nÄ± temizler.
	 * Neden: Bellek sÄ±zÄ±ntÄ±larÄ±nÄ± (memory leak) Ã¶nlemek iÃ§indir.
	 */
	public void cleanUp(){
		shader.cleanUp();
	}

	/**
	 * Ã‡izim iÅŸleminden Ã¶nce OpenGL ve shader ayarlarÄ±nÄ± hazÄ±rlar.
	 * @param camera Kamera bilgisi (matris hesaplamalarÄ± iÃ§in).
	 * @param lightDir IÅŸÄ±k yÃ¶nÃ¼.
	 * @param clipPlane KÄ±rpma dÃ¼zlemi.
	 */
	private void prepare(ICamera camera, scene.Scene scene, Vector4f clipPlane, org.lwjgl.util.vector.Matrix4f toShadowSpace) {
		shader.start();
		
		if (toShadowSpace != null) {
			shader.toShadowMapSpace.loadMatrix(toShadowSpace);
		}
		
		shader.projectionViewMatrix.loadMatrix(camera.getProjectionViewMatrix());
		shader.lightDirection.loadVec3(scene.getLightDirection());
		shader.lightColor.loadVec3(scene.getLightColor());
		shader.lightBrightness.loadFloat(scene.getLightBrightness());
		shader.ambientLight.loadFloat(scene.getAmbientLight());
		shader.plane.loadVec4(clipPlane);
		shader.cameraPosition.loadVec3(camera.getPosition());

		if (scene.getSky() != null && scene.getSky() instanceof skybox.atmosphere.AtmosphereSky) {
			skybox.atmosphere.AtmosphereSky sky = (skybox.atmosphere.AtmosphereSky) scene.getSky();
			shader.loadCloudShadowData(sky.getTime(), new org.lwjgl.util.vector.Vector2f(scene.getWindVelocity().x, scene.getWindVelocity().z), sky.isCloudsEnabled(), sky.getClusters());
		} else {
			shader.loadCloudShadowData(0f, new org.lwjgl.util.vector.Vector2f(0, 0), false, null);
		}
		
		// EÄŸer sahnede nokta Ä±ÅŸÄ±k varsa shader'a yÃ¼kle, yoksa siyah renk (0,0,0) gÃ¶nder
		java.util.List<scene.Light> pointLights = scene.getPointLights();
		for (int i = 0; i < 4; i++) {
			if (i < pointLights.size() && pointLights.get(i) != null) {
				shader.pointLightPos[i].loadVec3(pointLights.get(i).getPosition());
				shader.pointLightColor[i].loadVec3(pointLights.get(i).getColor());
				shader.pointLightAttenuation[i].loadVec3(pointLights.get(i).getAttenuation());
			} else {
				shader.pointLightColor[i].loadVec3(new Vector3f(0, 0, 0));
				shader.pointLightAttenuation[i].loadVec3(new Vector3f(1, 0, 0)); // 0'a bÃ¶lÃ¼nme hatasÄ±nÄ± Ã¶nlemek iÃ§in sabit 1 gÃ¶nder!
			}
		}
		
		OpenGlUtils.antialias(true);
		OpenGlUtils.disableBlending();
		OpenGlUtils.enableDepthTesting(true);
	}

	/**
	 * Ã‡izim iÅŸlemi bittikten sonra shader programÄ±nÄ± durdurur.
	 */
	private void finish() {
		shader.stop();
		org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE1);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
	}

	/**
	 * Modele ait kaplama (texture) Ã¶zelliklerini shader'a yÃ¼kler.
	 * Neden: Her modelin kendine ait resmi ve parlama haritasÄ± (glow map vb.) olabilir, bunlarÄ± hazÄ±rlamak gerekir.
	 * @param skin Modele ait kaplama verisi.
	 */
	private void prepareSkin(Skin skin) {
		skin.getDiffuseTexture().bindToUnit(0);
		if (skin.hasExtraMap()) {
			skin.getExtraInfoMap().bindToUnit(1);
		}
		shader.hasExtraMap.loadBoolean(skin.hasExtraMap());
		shader.useFakeLighting.loadBoolean(skin.isUseFakeLighting());
		OpenGlUtils.cullBackFaces(skin.isCullBackFaces());
	}

}

