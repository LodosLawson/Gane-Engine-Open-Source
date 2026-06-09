package environmentMapRenderer;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Vector3f;

import renderEngine.MasterRenderer;
import scene.Scene;
import textures.Texture;

/**
 * Çevresel (Environment) Haritalarını (örneğin yansımalar için çevredeki cisimlerin görünümü) oluşturmak
 * ve render etmek için kullanılan sınıftır. Belirtilen merkezden her yöne bakıp sahneyi küp dokusuna yazar.
 */
public class EnviroMapRenderer {

	/**
	 * Çevresel küp haritasını (Cube Map) çizen ana fonksiyondur.
	 * Neden: Yansıtıcı yüzeylerin (örneğin ayna, parlak küre) üzerinde çevrenin yansımasını 
	 * dinamik olarak oluşturmak için kullanılır.
	 * 
	 * @param cubeMap Çizimin kaydedileceği küp kaplaması (texture).
	 * @param scene Çizilecek sahnenin ta kendisi.
	 * @param center Çizimin/yansımanın merkezi. (Genellikle yansıma yapan objenin konumu).
	 * @param renderer Ekrana düşük çözünürlüklü çizim yapmak için kullanılacak ana çizici (MasterRenderer).
	 */
	public static void renderEnvironmentMap(Texture cubeMap, Scene scene, Vector3f center, MasterRenderer renderer) {

		CubeMapCamera camera = new CubeMapCamera(center);

		// Framebuffer (FBO) oluştur. Normalde ekrana çizilen görüntüyü bir dokuya/texture'a çizmemizi sağlar.
		int fbo = GL30.glGenFramebuffers();
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
		GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);

		// Derinlik tamponu (Depth buffer) ekle, böylece uzak-yakın nesne hesaplaması doğru çalışır.
		int depthBuffer = GL30.glGenRenderbuffers();
		GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthBuffer);
		GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL14.GL_DEPTH_COMPONENT24, cubeMap.size, cubeMap.size);
		GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER,
				depthBuffer);

		// Görüş alanını (Viewport) küp yüzeyinin boyutlarına göre ayarla.
		GL11.glViewport(0, 0, cubeMap.size, cubeMap.size);

		// Küpün 6 yüzeyi için döngü oluştur (Sağ, Sol, Üst, Alt, Ön, Arka).
		for (int i = 0; i < 6; i++) {

			// FBO'nun çizim hedefini küp haritasının (cube map) şu anki yüzeyi olarak belirle.
			GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
					GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, cubeMap.textureId, 0);
			
			// Kamerayı ilgili yöne çevir (Örn: Sola bak, sağa bak).
			camera.switchToFace(i);
			
			// Sahneyi FBO'ya (ve dolayısıyla küp haritasının bu yüzüne) çiz.
			renderer.renderLowQualityScene(scene, camera);

		}
		
		// FBO kullanımını durdur ve ekran (default framebuffer) çizimine geri dön.
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
		// Çözünürlüğü ekran boyutlarına tekrar geri getir.
		GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
		
		// Geçici olarak oluşturduğumuz fbo ve derinlik tamponlarını hafızadan sil.
		GL30.glDeleteRenderbuffers(depthBuffer);
		GL30.glDeleteFramebuffers(fbo);
		
		// Oluşturulan küp haritasını bağla ve mimap'leri (uzaklık hesaplaması için küçültülmüş resimleri) üret.
		cubeMap.bindToUnit(0);
		GL30.glGenerateMipmap(GL13.GL_TEXTURE_CUBE_MAP);

	}

}
