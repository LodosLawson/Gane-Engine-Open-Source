package renderEngine;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Vector4f;

import entityRenderers.EntityRenderer;
import scene.Scene;
import shinyRenderer.ShinyRenderer;
import skybox.SkyboxRenderer;
import utils.ICamera;
import water.WaterFrameBuffers;
import water.WaterRenderer;

/**
 * Birden fazla özelleşmiş renderer sınıfını (Su, Gökyüzü, Objeler, Parlak Objeler) tek bir çatı altında
 * toplayan ve doğru sıralamayla çizmelerini sağlayan ana yönetici sınıftır.
 */
public class MasterRenderer {
	
	// Hiçbir kırpma (clipping) düzleminin aktif olmadığını belirten sabit (X,Y,Z,W)
	private static final Vector4f NO_CLIP = new Vector4f(0, 0, 0, 1);

	// Standart 3B objelerin çizicisi
	private EntityRenderer entityRenderer;
	// Dinamik yansıma haritasına sahip parlak objelerin çizicisi
	private ShinyRenderer shinyRenderer;
	// Arka plan gökyüzü (Skybox) çizicisi
	private SkyboxRenderer skyRenderer;
	// Gerçekçi su yüzeyi çizicisi
	private WaterRenderer waterRenderer;
	// Suyun kırılma ve yansıma işlemlerini depolayan Frame Buffer Object (FBO) yöneticisi
	private WaterFrameBuffers waterFbos;

	/**
	 * Tüm alt render sistemlerini içeri alarak MasterRenderer'ı başlatır.
	 */
	protected MasterRenderer(EntityRenderer entityRenderer, SkyboxRenderer skyRenderer, WaterRenderer waterRenderer, WaterFrameBuffers waterFbos, ShinyRenderer shinyRenderer) {
		this.entityRenderer = entityRenderer;
		this.skyRenderer = skyRenderer;
		this.waterRenderer = waterRenderer;
		this.waterFbos = waterFbos;
		this.shinyRenderer = shinyRenderer;
	}
	
	/**
	 * Düşük kaliteli, basitleştirilmiş bir sahne renderı alır. 
	 * Genellikle Environment Map (Küp Haritası) gibi su ve parlama detayına ihtiyaç duymayan 
	 * geçici dokular yaratmak için kullanılır.
	 * 
	 * @param scene Çizilecek sahne verisi
	 * @param cubeMapCamera Kameranın bakış açısını temsil eden objesi (6 yöne bakar)
	 */
	public void renderLowQualityScene(Scene scene, ICamera cubeMapCamera){
		prepare();
		// Sadece temel objeler ve gökyüzü çizilir. Su veya parlak nesneler çizilmez.
		entityRenderer.render(scene.getImportantEntities(), cubeMapCamera, scene.getLightDirection(), NO_CLIP);
		if (scene.getSky() != null) {
			skyRenderer.render(scene.getSky(), cubeMapCamera);
		}
	}

	/**
	 * Tam kalite ana sahneyi çizer.
	 * Önce su efektleri için gizli (off-screen) yansıma/kırılma hesaplamaları yapılır,
	 * ardından asıl oyun görüntüsü ekrana basılır.
	 * 
	 * @param scene Çizilecek olan asıl sahne verisi
	 */
	protected void renderScene(Scene scene) {
		
		// Kırpma düzlemi aktif hale getirilir (Su yansımaları için su seviyesinin altı kesilir vs.)
		GL11.glEnable(GL30.GL_CLIP_DISTANCE0);
		// 1. Aşama: Su yüzeyinin altındaki nesnelerin kırılma haritasını hesapla
		renderWaterRefractionPass(scene);
		// 2. Aşama: Su yüzeyinin üstündeki nesnelerin yansıma haritasını hesapla
		renderWaterReflectionPass(scene);
		GL11.glDisable(GL30.GL_CLIP_DISTANCE0);
		
		// 3. Aşama: Her şey hazır, asıl sahneyi oluştur ve ekrana çiz
		renderMainPass(scene);

	}

	/**
	 * Bütün alt render sınıflarına ait Shader'ları ve bellekteki VAO/VBO nesnelerini temizler.
	 */
	protected void cleanUp() {
		entityRenderer.cleanUp();
		skyRenderer.cleanUp();
		waterRenderer.cleanUp();
		shinyRenderer.cleanUp();
	}

	/**
	 * Her bir çizim aşamasından önce ekranı (veya FBO'yu) temizler ve arka plan rengini beyaz (1,1,1,1) yapar.
	 * Derinlik (Z-Buffer) ve Renk buffer'ları sıfırlanır.
	 */
	private void prepare() {
		// Koyu gri arka plan — UI elemanlarının görünür olması için
		GL11.glClearColor(0.12f, 0.12f, 0.15f, 1);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
	}
	
	/**
	 * Suyun yansıma haritasını (Reflection) hazırlayan ara geçiş aşaması.
	 */
	private void renderWaterReflectionPass(Scene scene){
		// Çizimleri ekrana değil, yansıma FBO'suna yönlendir
		waterFbos.bindReflectionFrameBuffer();
		prepare();
		// Kamerayı suyun altına göre simetrik çevir (yansıma efekti için hile)
		scene.getCamera().reflect(scene.getWaterHeight());
		
		// Kameraya göre ters dönmüş dünyayı, suyun altını kırparak çiz (clipPlane: 0,1,0)
		entityRenderer.render(scene.getReflectedEntities(), scene.getCamera(), scene.getLightDirection(), new Vector4f(0,1,0,0.1f));
		if (scene.getSky() != null) {
			skyRenderer.render(scene.getSky(), scene.getCamera());
		}
		
		// Yansıma çizimi bitti, tekrar ana ekrana dön
		waterFbos.unbindCurrentFrameBuffer();
		// Kamerayı asıl konumuna geri döndür
		scene.getCamera().reflect(scene.getWaterHeight());
	}
	
	/**
	 * Suyun kırılma haritasını (Refraction) hazırlayan ara geçiş aşaması.
	 */
	private void renderWaterRefractionPass(Scene scene){
		// Çizimleri kırılma FBO'suna yönlendir
		waterFbos.bindRefractionFrameBuffer();
		prepare();
		// Suyun sadece altındaki nesneleri çiz (clipPlane: 0,-1,0)
		entityRenderer.render(scene.getUnderwaterEntities(), scene.getCamera(), scene.getLightDirection(), new Vector4f(0,-1,0, 0));
		waterFbos.unbindCurrentFrameBuffer();
	}
	
	/**
	 * Hazırlanan tüm yansıma, kırılma ve gölge haritalarını kullanarak 
	 * oyuncunun gördüğü son ana görüntüyü çizen aşama.
	 */
	private void renderMainPass(Scene scene){
		prepare();
		// Normal objeleri çiz
		entityRenderer.render(scene.getAllEntities(), scene.getCamera(), scene.getLightDirection(), NO_CLIP);
		// Çevresel yansımalara sahip parlak metal/cam tarzı objeleri çiz
		shinyRenderer.render(scene.getShinyEntities(), scene.getEnvironmentMap(), scene.getCamera(), scene.getLightDirection());
		// Gökyüzünü arka plana oturt
		if (scene.getSky() != null) {
			skyRenderer.render(scene.getSky(), scene.getCamera());
		}
		// En son, hazırlanan reflection/refraction dokularıyla su yüzeyini çiz
		waterRenderer.render(scene.getWater(), scene.getCamera(), scene.getLightDirection());
	}

}
