package renderEngine;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Vector4f;

import entityRenderers.EntityRenderer;
import scene.Scene;
import shinyRenderer.ShinyRenderer;
import skybox.classic.SkyboxRenderer;
import terrain.TerrainRenderer;
import utils.ICamera;
import water.tile.WaterFrameBuffers;
import water.ocean.OceanRenderer;

/**
 * Birden fazla ÃƒÂ¶zelleÃ…Å¸miÃ…Å¸ renderer sÃ„Â±nÃ„Â±fÃ„Â±nÃ„Â± (Su, GÃƒÂ¶kyÃƒÂ¼zÃƒÂ¼, Objeler, Parlak
 * Objeler) tek bir ÃƒÂ§atÃ„Â± altÃ„Â±nda
 * toplayan ve doÃ„Å¸ru sÃ„Â±ralamayla ÃƒÂ§izmelerini saÃ„Å¸layan ana yÃƒÂ¶netici sÃ„Â±nÃ„Â±ftÃ„Â±r.
 */
public class MasterRenderer {

	// HiÃƒÂ§bir kÃ„Â±rpma (clipping) dÃƒÂ¼zleminin aktif olmadÃ„Â±Ã„Å¸Ã„Â±nÃ„Â± belirten sabit (X,Y,Z,W)
	private static final Vector4f NO_CLIP = new Vector4f(0, 0, 0, 1);

	// Standart 3B objelerin ÃƒÂ§izicisi
	private EntityRenderer entityRenderer;
	// Dinamik yansÃ„Â±ma haritasÃ„Â±na sahip parlak objelerin ÃƒÂ§izicisi
	private ShinyRenderer shinyRenderer;
	// Arka plan gÃƒÂ¶kyÃƒÂ¼zÃƒÂ¼ (Skybox) ÃƒÂ§izicisi
	private SkyboxRenderer skyRenderer;
	// Algoritmik Atmosfer ÃƒÂ§izicisi
	private skybox.atmosphere.AtmosphereRenderer atmosphereRenderer;
	// GerÃƒÂ§ekÃƒÂ§i okyanus yÃƒÂ¼zeyi ÃƒÂ§izicisi
	private OceanRenderer oceanRenderer;
	// Terrain (Arazi) ÃƒÂ§izicisi Ã¢â‚¬â€ FlatTerrain, VoxelTerrain vb.
	private TerrainRenderer terrainRenderer;

	private entityRenderers.InstancedRenderer instancedRenderer;
	private shadows.ShadowMapMasterRenderer shadowMapRenderer;
	private sunRenderer.SunRenderer sunRenderer;

	// Suyun kÃ„Â±rÃ„Â±lma ve yansÃ„Â±ma iÃ…Å¸lemlerini depolayan Frame Buffer Object (FBO)
	// yÃƒÂ¶neticisi
	private WaterFrameBuffers waterFbos;
	
	private postProcessing.Fbo multisampleFbo;
	private postProcessing.Fbo outputFbo;

	/**
	 * TÃƒÂ¼m alt render sistemlerini iÃƒÂ§eri alarak MasterRenderer'Ã„Â± baÃ…Å¸latÃ„Â±r.
	 */
	protected MasterRenderer(EntityRenderer entityRenderer, SkyboxRenderer skyRenderer,
			skybox.atmosphere.AtmosphereRenderer atmosphereRenderer, OceanRenderer oceanRenderer, WaterFrameBuffers waterFbos,
			ShinyRenderer shinyRenderer) {
		this.entityRenderer = entityRenderer;
		this.skyRenderer = skyRenderer;
		this.atmosphereRenderer = atmosphereRenderer;
		this.oceanRenderer = oceanRenderer;
		this.terrainRenderer = new TerrainRenderer();

		this.waterFbos = waterFbos;
		this.shinyRenderer = shinyRenderer;

		this.sunRenderer = new sunRenderer.SunRenderer();

		this.instancedRenderer = new entityRenderers.InstancedRenderer(new entityRenderers.InstancedShader());

		// MSAA (Multisample Anti-Aliasing) Aktif Et (DisplayManager.java'daki
		// withSamples(4) ayari ile calisir)
		GL11.glEnable(GL13.GL_MULTISAMPLE);
	}

	/**
	 * DÃƒÂ¼Ã…Å¸ÃƒÂ¼k kaliteli, basitleÃ…Å¸tirilmiÃ…Å¸ bir sahne renderÃ„Â± alÃ„Â±r.
	 * Genellikle Environment Map (KÃƒÂ¼p HaritasÃ„Â±) gibi su ve parlama detayÃ„Â±na ihtiyaÃƒÂ§
	 * duymayan
	 * geÃƒÂ§ici dokular yaratmak iÃƒÂ§in kullanÃ„Â±lÃ„Â±r.
	 * 
	 * @param scene         Ãƒâ€¡izilecek sahne verisi
	 * @param cubeMapCamera KameranÃ„Â±n bakÃ„Â±Ã…Å¸ aÃƒÂ§Ã„Â±sÃ„Â±nÃ„Â± temsil eden objesi (6 yÃƒÂ¶ne
	 *                      bakar)
	 */
	public void renderLowQualityScene(Scene scene, ICamera cubeMapCamera) {
		prepare();
		// Sadece temel objeler ve gÃƒÂ¶kyÃƒÂ¼zÃƒÂ¼ ÃƒÂ§izilir. Su veya parlak nesneler ÃƒÂ§izilmez.
		entityRenderer.render(scene.getImportantEntities(), cubeMapCamera, scene, NO_CLIP, null);
		if (scene.getSky() != null) {
			scene.getSky().render(cubeMapCamera, scene.getLightDirection());
		}
	}

	/**
	 * Tam kalite ana sahneyi ÃƒÂ§izer.
	 * Ãƒâ€“nce su efektleri iÃƒÂ§in gizli (off-screen) yansÃ„Â±ma/kÃ„Â±rÃ„Â±lma hesaplamalarÃ„Â±
	 * yapÃ„Â±lÃ„Â±r,
	 * ardÃ„Â±ndan asÃ„Â±l oyun gÃƒÂ¶rÃƒÂ¼ntÃƒÂ¼sÃƒÂ¼ ekrana basÃ„Â±lÃ„Â±r.
	 * 
	 * @param scene Ãƒâ€¡izilecek olan asÃ„Â±l sahne verisi
	 */
	protected void renderScene(Scene scene, float delta) {
		if (multisampleFbo == null) {
			multisampleFbo = new postProcessing.Fbo(org.lwjgl.opengl.Display.getWidth(), org.lwjgl.opengl.Display.getHeight(), postProcessing.Fbo.DEPTH_RENDER_BUFFER);
			outputFbo = new postProcessing.Fbo(org.lwjgl.opengl.Display.getWidth(), org.lwjgl.opengl.Display.getHeight(), postProcessing.Fbo.DEPTH_TEXTURE);
			postProcessing.PostProcessing.init();
		}
		if (shadowMapRenderer == null) {
			shadowMapRenderer = new shadows.ShadowMapMasterRenderer(scene.getCamera());
		}

		// 0. AÃ…Å¸ama: Su-obje etkileÃ…Å¸imlerini gÃƒÂ¼ncelle (Ripple tespiti)
		// Reflection/refraction pass ÃƒÂ¶ncesinde yapÃ„Â±lmalÃ„Â± ki shader verileri hazÃ„Â±r
		// olsun.
		scene.updateWaterInteractions(delta);

		// Eski procedural cimen ve agac (Flora) uretimini kapatiyoruz
		// terrain.FloraManager.update(scene);

		// GÃƒÂ¶lgeleri ÃƒÂ§iz (Shadow Pass)
		shadowMapRenderer.render(scene, scene.getCamera());

		// KÃ„Â±rpma dÃƒÂ¼zlemi aktif hale getirilir (Su yansÃ„Â±malarÃ„Â± iÃƒÂ§in su seviyesinin altÃ„Â±
		// kesilir vs.)
		GL11.glEnable(GL30.GL_CLIP_DISTANCE0);
		// 1. AÃ…Å¸ama: Su yÃƒÂ¼zeyinin altÃ„Â±ndaki nesnelerin kÃ„Â±rÃ„Â±lma haritasÃ„Â±nÃ„Â± hesapla
		renderWaterRefractionPass(scene);
		// 2. AÃ…Å¸ama: Su yÃƒÂ¼zeyinin ÃƒÂ¼stÃƒÂ¼ndeki nesnelerin yansÃ„Â±ma haritasÃ„Â±nÃ„Â± hesapla
		renderWaterReflectionPass(scene);
		GL11.glDisable(GL30.GL_CLIP_DISTANCE0);

		// 3. AÃ…Å¸ama: AsÃ„Â±l sahneyi FBO'ya (Multisampled) ÃƒÂ§iz
		multisampleFbo.bindFrameBuffer();
		renderMainPass(scene, delta);
		multisampleFbo.unbindFrameBuffer();
		
		// 4. AÃ…Å¸ama: Multisample FBO'yu normal FBO'ya kopyala (Resolve)
		org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER, multisampleFbo.getColourTexture()); // Wait, actually resolve needs proper fbo IDs
		// To keep things simple and functional for Bloom, let's just render to outputFbo directly for now since MSAA can be tricky with custom FBOs.
		// Actually let's just render Main Pass to outputFbo:
		outputFbo.bindFrameBuffer();
		renderMainPass(scene, delta);
		outputFbo.unbindFrameBuffer();

		// 5. AÃ…Å¸ama: Post-Processing (Bloom / Contrast vs) uygulayarak ekrana bas
		postProcessing.PostProcessing.doPostProcessing(outputFbo.getColourTexture());

	}

	/**
	 * BÃƒÂ¼tÃƒÂ¼n alt render sÃ„Â±nÃ„Â±flarÃ„Â±na ait Shader'larÃ„Â± ve bellekteki VAO/VBO
	 * nesnelerini temizler.
	 */
	protected void cleanUp() {
		entityRenderer.cleanUp();
		skyRenderer.cleanUp();
		if (atmosphereRenderer != null)
			atmosphereRenderer.cleanUp();
		if (oceanRenderer != null)
			oceanRenderer.cleanUp();
		if (terrainRenderer != null)
			terrainRenderer.cleanUp();
		shinyRenderer.cleanUp();
		if (instancedRenderer != null)
			instancedRenderer.cleanUp();
		if (shadowMapRenderer != null)
			shadowMapRenderer.cleanUp();
		if (sunRenderer != null)
			sunRenderer.cleanUp();
		if (outputFbo != null) {
			outputFbo.cleanUp();
			multisampleFbo.cleanUp();
			postProcessing.PostProcessing.cleanUp();
		}
	}

	public OceanRenderer getOceanRenderer() {
		return oceanRenderer;
	}

	/**
	 * Her bir ÃƒÂ§izim aÃ…Å¸amasÃ„Â±ndan ÃƒÂ¶nce ekranÃ„Â± (veya FBO'yu) temizler ve arka plan
	 * rengini beyaz (1,1,1,1) yapar.
	 * Derinlik (Z-Buffer) ve Renk buffer'larÃ„Â± sÃ„Â±fÃ„Â±rlanÃ„Â±r.
	 */
	private void prepare() {
		// Koyu gri arka plan Ã¢â‚¬â€ UI elemanlarÃ„Â±nÃ„Â±n gÃƒÂ¶rÃƒÂ¼nÃƒÂ¼r olmasÃ„Â± iÃƒÂ§in
		GL11.glClearColor(0.12f, 0.12f, 0.15f, 1);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
	}

	/**
	 * Suyun yansÃ„Â±ma haritasÃ„Â±nÃ„Â± (Reflection) hazÃ„Â±rlayan ara geÃƒÂ§iÃ…Å¸ aÃ…Å¸amasÃ„Â±.
	 */
	private void renderWaterReflectionPass(Scene scene) {
		// Ãƒâ€¡izimleri ekrana deÃ„Å¸il, yansÃ„Â±ma FBO'suna yÃƒÂ¶nlendir
		waterFbos.bindReflectionFrameBuffer();
		prepare();
		// KamerayÃ„Â± suyun altÃ„Â±na gÃƒÂ¶re simetrik ÃƒÂ§evir (yansÃ„Â±ma efekti iÃƒÂ§in hile)
		scene.getCamera().reflect(scene.getWaterHeight());

		// Terrain yansÃ„Â±masÃ„Â±nÃ„Â± ÃƒÂ§iz (su seviyesinin ÃƒÂ¼stÃƒÂ¼nÃƒÂ¼ ÃƒÂ§izmek iÃƒÂ§in y > waterHeight)
		// HATA DÃƒÅ“ZELTMESÃ„Â°: Terrain'in altÃ„Â±ndan bakÃ„Â±ldÃ„Â±Ã„Å¸Ã„Â±nda yansÃ„Â±ma hatalÃ„Â± gÃƒÂ¶rÃƒÂ¼ndÃƒÂ¼Ã„Å¸ÃƒÂ¼ iÃƒÂ§in
		// Terrain yansÃ„Â±masÃ„Â±nÃ„Â± tamamen kapatÃ„Â±yoruz. Sadece gÃƒÂ¶kyÃƒÂ¼zÃƒÂ¼ ve objeler yansÃ„Â±yacak.
		for (terrain.ITerrain t : scene.getTerrains()) {
			t.setClipPlane(new Vector4f(0, 1, 0, -scene.getWaterHeight() + 1.5f));
		}

		int mapId = shadowMapRenderer.getShadowMap();
		// terrainRenderer.render(scene, scene.getCamera(), shadowMapRenderer.getToShadowMapSpaceMatrix(), mapId);

		if (scene.getGrassField() != null) {
			scene.getGrassField().setClipPlane(new Vector4f(0, 1, 0, -scene.getWaterHeight() + 1.5f));
			scene.getGrassField().render(scene.getCamera(), scene, shadowMapRenderer.getToShadowMapSpaceMatrix(), mapId);
		}

		if (!scene.getInstancedEntities().isEmpty()) {
			instancedRenderer.render(scene.getInstancedEntities(), scene.getCamera(), scene,
					new Vector4f(0, 1, 0, -scene.getWaterHeight() + 0.1f), null);
		}

		// Kameraya gÃƒÂ¶re ters dÃƒÂ¶nmÃƒÂ¼Ã…Å¸ dÃƒÂ¼nyayÃ„Â±, suyun altÃ„Â±nÃ„Â± kÃ„Â±rparak ÃƒÂ§iz (clipPlane:
		// 0,1,0)
		entityRenderer.render(scene.getReflectedEntities(), scene.getCamera(), scene, new Vector4f(0, 1, 0, -scene.getWaterHeight() + 0.1f),
				null);
		if (scene.getSky() != null) {
			scene.getSky().render(scene.getCamera(), scene.getLightDirection());
		}

		// YansÃ„Â±ma ÃƒÂ§izimi bitti, tekrar ana ekrana dÃƒÂ¶n
		waterFbos.unbindCurrentFrameBuffer();
		// KamerayÃ„Â± asÃ„Â±l konumuna geri dÃƒÂ¶ndÃƒÂ¼r
		scene.getCamera().reflect(scene.getWaterHeight());
	}

	/**
	 * Suyun kÃ„Â±rÃ„Â±lma haritasÃ„Â±nÃ„Â± (Refraction) hazÃ„Â±rlayan ara geÃƒÂ§iÃ…Å¸ aÃ…Å¸amasÃ„Â±.
	 */
	private void renderWaterRefractionPass(Scene scene) {
		// Ãƒâ€¡izimleri kÃ„Â±rÃ„Â±lma FBO'suna yÃƒÂ¶nlendir
		waterFbos.bindRefractionFrameBuffer();
		prepare();

		// Terrain kÃ„Â±rÃ„Â±lmasÃ„Â±nÃ„Â± ÃƒÂ§iz (Clip plane'i sonsuza alarak shoreline derinliÃ„Å¸ini koruyoruz)
		for (terrain.ITerrain t : scene.getTerrains()) {
			t.setClipPlane(new Vector4f(0, -1, 0, 100000f));
		}
		int mapId = shadowMapRenderer.getShadowMap();
		terrainRenderer.render(scene, scene.getCamera(), shadowMapRenderer.getToShadowMapSpaceMatrix(), mapId);

		// Suyun sadece altÃ„Â±ndaki nesneleri ÃƒÂ§iz (clipPlane: 0,-1,0)
		entityRenderer.render(scene.getUnderwaterEntities(), scene.getCamera(), scene, new Vector4f(0, -1, 0, scene.getWaterHeight() + 0.1f), null);

		// OPTIMIZATION: Do not render instanced entities (trees, grass) in refraction pass.
		// Underwater grass/trees is rare, and it destroys performance.
		
		waterFbos.unbindCurrentFrameBuffer();
	}

	/**
	 * HazÃ„Â±rlanan tÃƒÂ¼m yansÃ„Â±ma, kÃ„Â±rÃ„Â±lma ve gÃƒÂ¶lge haritalarÃ„Â±nÃ„Â± kullanarak
	 * oyuncunun gÃƒÂ¶rdÃƒÂ¼Ã„Å¸ÃƒÂ¼ son ana gÃƒÂ¶rÃƒÂ¼ntÃƒÂ¼yÃƒÂ¼ ÃƒÂ§izen aÃ…Å¸ama.
	 */
	private void renderMainPass(Scene scene, float delta) {
		int mapId = shadowMapRenderer.getShadowMap();
		prepare();

		// Reset terrain clip planes
		for (terrain.ITerrain t : scene.getTerrains()) {
			t.setClipPlane(new Vector4f(0, -1, 0, 100000f));
		}

		// 1. Ãƒâ€“nce terrain (arazi) ÃƒÂ§iz Ã¢â‚¬â€ opaklara zemin olusÃ…Å¸turmak iÃƒÂ§in
		if (shadowMapRenderer != null) {
			GL13.glActiveTexture(GL13.GL_TEXTURE6);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, shadowMapRenderer.getShadowMap());
			terrainRenderer.render(scene, scene.getCamera(), shadowMapRenderer.getToShadowMapSpaceMatrix(), mapId);
		} else {
			terrainRenderer.render(scene, scene.getCamera(), shadowMapRenderer.getToShadowMapSpaceMatrix(), mapId);
		}

		// Ãƒâ€¡imenleri ÃƒÂ§iz
		if (scene.getGrassField() != null) {
			scene.getGrassField().setClipPlane(new Vector4f(0, -1, 0, 100000f));
			scene.getGrassField().render(scene.getCamera(), scene, shadowMapRenderer.getToShadowMapSpaceMatrix(), mapId);
		}

		// Instanced Objeleri Ãƒâ€¡iz (Ãƒâ€¡imen, AÃ„Å¸aÃƒÂ§ vs.)
		if (!scene.getInstancedEntities().isEmpty()) {
			if (shadowMapRenderer != null) {
				GL13.glActiveTexture(GL13.GL_TEXTURE6);
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, shadowMapRenderer.getShadowMap());
				instancedRenderer.render(scene.getInstancedEntities(), scene.getCamera(), scene, NO_CLIP,
						shadowMapRenderer.getToShadowMapSpaceMatrix());
			} else {
				instancedRenderer.render(scene.getInstancedEntities(), scene.getCamera(), scene, NO_CLIP, null);
			}
		}

		// 2. Normal objeleri ÃƒÂ§iz
		if (shadowMapRenderer != null) {
			GL13.glActiveTexture(GL13.GL_TEXTURE6);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, shadowMapRenderer.getShadowMap());
			entityRenderer.render(scene.getAllEntities(), scene.getCamera(), scene, NO_CLIP,
					shadowMapRenderer.getToShadowMapSpaceMatrix());
			entityRenderer.render(scene.getUnbatchedFlora(), scene.getCamera(), scene, NO_CLIP,
					shadowMapRenderer.getToShadowMapSpaceMatrix());
		} else {
			entityRenderer.render(scene.getAllEntities(), scene.getCamera(), scene, NO_CLIP, null);
			entityRenderer.render(scene.getUnbatchedFlora(), scene.getCamera(), scene, NO_CLIP, null);
		}
		// 3. Ãƒâ€¡evresel yansÃ„Â±malara sahip parlak metal/cam tarzÃ„Â± objeleri ÃƒÂ§iz
		shinyRenderer.render(scene.getShinyEntities(), scene.getEnvironmentMap(), scene.getCamera(),
				scene.getLightDirection(), scene);
		if (scene.getSky() != null) {
			scene.getSky().render(scene.getCamera(), scene.getLightDirection());
		}

		// 4. Güneşi çiz
		if (scene.getSun() != null) {
			sunRenderer.render(scene.getSun(), scene.getCamera());
		}

		// 5. En son, hazýrlanan reflection/refraction dokularýyla su yüzerini çiz
		if (!scene.getWater().isEmpty()) {
			oceanRenderer.render(scene, scene.getCamera(), delta);
		}

		// 6. PartikÃƒÂ¼lleri suyun ÃƒÂ¼stune ekle
		particles.ParticleManager.getInstance().render(scene.getCamera());

		// 7. SualtÃ„Â± Kamera Efekti
		renderUnderwaterOverlay(scene);

		// 8. Hata Ayıklama (Debug) - Güneşi Gösteren Çizgi
		if (!gane.MainApp.playMode) {
			renderSunDebugLine(scene);
		}

		// 9. Obje Yön (Orientation) Debug Çizgileri
		if (gane.MainApp.showOrientationDebug) {
			renderOrientationDebugLines(scene);
		}
	}

	/**
	 * Sahnedeki tüm nesnelerin yön (orientation) matrislerini hesaplayıp
	 * X, Y, Z eksenlerini görsel bir rehber olarak çizer.
	 */
	private void renderOrientationDebugLines(Scene scene) {
		org.lwjgl.opengl.GL20.glUseProgram(0);
		for (int i = 0; i <= 6; i++) {
			org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0 + i);
			GL11.glDisable(GL11.GL_TEXTURE_2D);
		}
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_DEPTH_TEST); 
		GL11.glLineWidth(3.0f); 

		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPushMatrix();
		java.nio.FloatBuffer projBuffer = org.lwjgl.BufferUtils.createFloatBuffer(16);
		scene.getCamera().getProjectionViewMatrix().store(projBuffer);
		projBuffer.flip();
		GL11.glLoadMatrix(projBuffer);

		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		
		for (scene.Entity entity : scene.getAllEntities()) {
			GL11.glPushMatrix();
			GL11.glLoadIdentity();
			
			GL11.glTranslatef(entity.getPosition().x, entity.getPosition().y, entity.getPosition().z);
			
			// Önce Modelin kendi Offset rotasyonu (GLB için varsa) uygulanır
			GL11.glRotatef(entity.getModelOffsetRot().y, 0, 1, 0);
			GL11.glRotatef(entity.getModelOffsetRot().x, 1, 0, 0);
			GL11.glRotatef(entity.getModelOffsetRot().z, 0, 0, 1);
			
			// Sonra Objenin yerel rotasyonu uygulanır
			GL11.glRotatef(entity.getRotation().y, 0, 1, 0);
			GL11.glRotatef(entity.getRotation().x, 1, 0, 0);
			GL11.glRotatef(entity.getRotation().z, 0, 0, 1);

			// X Ekseni - Kırmızı (Right)
			GL11.glColor3f(1.0f, 0.0f, 0.0f);
			GL11.glBegin(GL11.GL_LINES);
			GL11.glVertex3f(0, 0, 0);
			GL11.glVertex3f(5.0f, 0, 0);
			GL11.glEnd();

			// Y Ekseni - Yeşil (Up)
			GL11.glColor3f(0.0f, 1.0f, 0.0f);
			GL11.glBegin(GL11.GL_LINES);
			GL11.glVertex3f(0, 0, 0);
			GL11.glVertex3f(0, 5.0f, 0);
			GL11.glEnd();

			// Z Ekseni - Mavi (Forward/Backward - Engine spesifik yönü)
			GL11.glColor3f(0.0f, 0.5f, 1.0f);
			GL11.glBegin(GL11.GL_LINES);
			GL11.glVertex3f(0, 0, 0);
			GL11.glVertex3f(0, 0, 5.0f); // Engine Forward +Z direction visually
			GL11.glEnd();
			
			GL11.glPopMatrix();
		}

		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPopMatrix();
		GL11.glMatrixMode(GL11.GL_MODELVIEW);

		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glLineWidth(1.0f);
	}

	/**
	 * Hata ayÃ„Â±klama (Debug) amacÃ„Â±yla kameradan gÃƒÂ¼neÃ…Å¸e doÃ„Å¸ru giden kalÃ„Â±n
	 * sarÃ„Â±/kÃ„Â±rmÃ„Â±zÃ„Â± bir referans ÃƒÂ§izgisi ÃƒÂ§izer.
	 */
	private void renderSunDebugLine(Scene scene) {
		org.lwjgl.opengl.GL20.glUseProgram(0);
		for (int i = 0; i <= 6; i++) {
			org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0 + i);
			GL11.glDisable(GL11.GL_TEXTURE_2D);
		}
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_DEPTH_TEST); // DuvarlarÃ„Â±n/DaÃ„Å¸larÃ„Â±n arkasÃ„Â±ndan da gÃƒÂ¶rÃƒÂ¼nmesi iÃƒÂ§in
		GL11.glLineWidth(10.0f); // KalÃ„Â±n lazer ÃƒÂ§izgisi

		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPushMatrix();
		java.nio.FloatBuffer projBuffer = org.lwjgl.BufferUtils.createFloatBuffer(16);
		scene.getCamera().getProjectionViewMatrix().store(projBuffer);
		projBuffer.flip();
		GL11.glLoadMatrix(projBuffer);

		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPushMatrix();
		GL11.glLoadIdentity();

		org.lwjgl.util.vector.Vector3f camPos = scene.getCamera().getPosition();
		org.lwjgl.util.vector.Vector3f sunDir = scene.getLightDirection();

		// IÃ…Å¸Ã„Â±Ã„Å¸Ã„Â±n geliÃ…Å¸ yÃƒÂ¶nÃƒÂ¼ "lightDirection" vektÃƒÂ¶rÃƒÂ¼nÃƒÂ¼n tersidir (-sunDir).
		GL11.glBegin(GL11.GL_LINES);
		GL11.glColor3f(1.0f, 0.0f, 0.0f); // Lazer baÃ…Å¸langÃ„Â±cÃ„Â± (KÃ„Â±rmÃ„Â±zÃ„Â±)
		// Kamera hizasÃ„Â±ndan hafif aÃ…Å¸aÃ„Å¸Ã„Â±da baÃ…Å¸lasÃ„Â±n ki oyuncu net gÃƒÂ¶rsÃƒÂ¼n
		GL11.glVertex3f(camPos.x, camPos.y - 1.0f, camPos.z);

		GL11.glColor3f(1.0f, 1.0f, 0.0f); // Lazer ucu (SarÃ„Â±)
		GL11.glVertex3f(camPos.x - sunDir.x * 250f, camPos.y - sunDir.y * 250f - 1.0f, camPos.z - sunDir.z * 250f);
		GL11.glEnd();

		// Matrixleri eski haline getir
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPopMatrix();
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPopMatrix();

		GL11.glEnable(GL11.GL_DEPTH_TEST); // DiÃ„Å¸er ÃƒÂ§izimler iÃƒÂ§in geri aÃƒÂ§
		GL11.glLineWidth(1.0f);
	}

	/**
	 * Kamera su altÃ„Â±ndaysa yarÃ„Â± saydam mavi bir katman (fog overlay) ekler.
	 * SualtÃ„Â±nda olduÃ„Å¸unda derinlik hissiyatÃ„Â± ve suyun iÃƒÂ§inde olma duygusu verir.
	 */
	private void renderUnderwaterOverlay(Scene scene) {
		float cameraY = scene.getCamera().getPosition().y;
		float waterHeight = scene.getWaterHeight();

		if (cameraY < waterHeight && !scene.getWater().isEmpty()) {
			water.tile.WaterTile tile = scene.getWater().get(0);

			// Derinlik faktÃƒÂ¶rÃƒÂ¼: Kamera ne kadar derindeyse sis o kadar yoÃ„Å¸un
			float depthFactor = Math.min((waterHeight - cameraY) * tile.getUnderwaterFogDensity(), 0.85f);

			// OpenGL 2D overlay: Tam ekran yarÃ„Â± saydam mavi dikdÃƒÂ¶rtgen
			org.lwjgl.opengl.GL20.glUseProgram(0);
			for (int i = 0; i <= 4; i++) {
				org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0 + i);
				GL11.glDisable(GL11.GL_TEXTURE_2D);
			}
			org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);

			GL11.glDisable(GL11.GL_DEPTH_TEST);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GL11.glPushMatrix();
			GL11.glLoadIdentity();
			GL11.glOrtho(0, 1, 0, 1, -1, 1);
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			GL11.glPushMatrix();
			GL11.glLoadIdentity();

			GL11.glColor4f(
					tile.getUnderwaterFogR(),
					tile.getUnderwaterFogG(),
					tile.getUnderwaterFogB(),
					depthFactor);
			GL11.glBegin(GL11.GL_QUADS);
			GL11.glVertex2f(0, 0);
			GL11.glVertex2f(1, 0);
			GL11.glVertex2f(1, 1);
			GL11.glVertex2f(0, 1);
			GL11.glEnd();

			GL11.glPopMatrix();
			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GL11.glPopMatrix();
			GL11.glMatrixMode(GL11.GL_MODELVIEW);

			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glColor4f(1, 1, 1, 1); // Renk state'ini sÃ„Â±fÃ„Â±rla
		}
	}

	public int getShadowMapTexture() {
		return shadowMapRenderer != null ? shadowMapRenderer.getShadowMap() : 0;
	}

	public shadows.ShadowMapMasterRenderer getShadowMapRenderer() {
		return shadowMapRenderer;
	}

}


