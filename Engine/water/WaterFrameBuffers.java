package water;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;

/**
 * Su efekti için gerekli olan yansıma (Reflection) ve kırılma (Refraction)
 * Frame Buffer Object (FBO) işlemlerini yönetir.
 * Kameranın konumuna göre ayrı sahneleri renderlayıp bu dokuları suya uygular.
 */
public class WaterFrameBuffers {

	// Yansıma dokusunun çözünürlüğü
	protected static final int REFLECTION_WIDTH = 640;
	private static final int REFLECTION_HEIGHT = 360;
	
	// Kırılma dokusunun çözünürlüğü
	protected static final int REFRACTION_WIDTH = 1280;
	private static final int REFRACTION_HEIGHT = 720;

	private int reflectionFrameBuffer;
	private int reflectionTexture;
	private int reflectionDepthBuffer; // Derinlik tamponu (Renderbuffer)
	
	private int refractionFrameBuffer;
	private int refractionTexture;
	private int refractionDepthTexture; // Derinlik dokusu (Su derinliğini hesaplamak için)

	/**
	 * Oyun yüklenirken çağrılır. Gerekli FBO'ları oluşturur.
	 */
	public WaterFrameBuffers() {
		initialiseReflectionFrameBuffer();
		initialiseRefractionFrameBuffer();
	}

	/**
	 * Oyun kapanırken çağrılır. OpenGL belleğini temizler.
	 */
	public void cleanUp() {
		GL30.glDeleteFramebuffers(reflectionFrameBuffer);
		GL11.glDeleteTextures(reflectionTexture);
		GL30.glDeleteRenderbuffers(reflectionDepthBuffer);
		GL30.glDeleteFramebuffers(refractionFrameBuffer);
		GL11.glDeleteTextures(refractionTexture);
		GL11.glDeleteTextures(refractionDepthTexture);
	}

	/** Yansıma çizimi yapılmadan önce çağrılır. */
	public void bindReflectionFrameBuffer() {
		bindFrameBuffer(reflectionFrameBuffer, REFLECTION_WIDTH, REFLECTION_HEIGHT);
	}
	
	/** Kırılma çizimi yapılmadan önce çağrılır. */
	public void bindRefractionFrameBuffer() {
		bindFrameBuffer(refractionFrameBuffer, REFRACTION_WIDTH, REFRACTION_HEIGHT);
	}
	
	/**
	 * FBO'ya render işlemi bittikten sonra çağrılır ve normal ekrana (default FBO) geri dönülür.
	 */
	public void unbindCurrentFrameBuffer() {
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0); // 0, varsayılan ekrandır
		GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
		GL11.glFinish();
	}

	/** @return Yansıma dokusunun (texture) ID'si */
	public int getReflectionTexture() {
		return reflectionTexture;
	}
	
	/** @return Kırılma dokusunun (texture) ID'si */
	public int getRefractionTexture() {
		return refractionTexture;
	}
	
	/** @return Kırılma işlemi için oluşturulan derinlik dokusunun ID'si */
	public int getRefractionDepthTexture(){
		return refractionDepthTexture;
	}

	private void initialiseReflectionFrameBuffer() {
		reflectionFrameBuffer = createFrameBuffer();
		reflectionTexture = createTextureAttachment(REFLECTION_WIDTH, REFLECTION_HEIGHT);
		reflectionDepthBuffer = createDepthBufferAttachment(REFLECTION_WIDTH, REFLECTION_HEIGHT);
		unbindCurrentFrameBuffer();
	}
	
	private void initialiseRefractionFrameBuffer() {
		refractionFrameBuffer = createFrameBuffer();
		refractionTexture = createTextureAttachment(REFRACTION_WIDTH, REFRACTION_HEIGHT);
		// Kırılma için su derinliğini (suyun altındaki nesnenin mesafesi) bilmemiz gerektiğinden derinlik dokusu kullanırız
		refractionDepthTexture = createDepthTextureAttachment(REFRACTION_WIDTH, REFRACTION_HEIGHT);
		unbindCurrentFrameBuffer();
	}
	
	/**
	 * İlgili FBO'yu aktif hale getirir ve görünüm alanını (viewport) ayarlar.
	 */
	private void bindFrameBuffer(int frameBuffer, int width, int height){
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0); // Dokuların bağlantısını kes
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, frameBuffer);
		GL11.glViewport(0, 0, width, height); // FBO çözünürlüğüne göre ayarla
	}

	/**
	 * Yeni bir Frame Buffer Object (FBO) oluşturur.
	 */
	private int createFrameBuffer() {
		int frameBuffer = GL30.glGenFramebuffers();
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, frameBuffer);
		// Renderlanan rengin 0 numaralı attachment'a (bağlantı noktasına) gideceğini belirtiyoruz
		GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
		GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
		return frameBuffer;
	}

	/**
	 * FBO için renk dokusu eklentisi (Color Attachment) oluşturur.
	 */
	private int createTextureAttachment(int width, int height) {
		int texture = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
		// Boş bir doku oluştur
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, width, height,
				0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		// Dokuyu FBO'nun renk eklentisine bağla
		GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
				texture, 0);
		return texture;
	}
	
	/**
	 * FBO için derinlik dokusu eklentisi (Depth Texture Attachment) oluşturur.
	 * Ekrandaki nesnelerin su yüzeyine olan derinliğini okumak için kullanılır.
	 */
	private int createDepthTextureAttachment(int width, int height){
		int texture = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT32, width, height,
				0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (ByteBuffer) null);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
				texture, 0);
		return texture;
	}

	/**
	 * Sadece derinlik testi (Depth Test) yapmak için bir Renderbuffer eklentisi oluşturur.
	 * (Dokudan okuma gerekmeyen durumlarda performansı artırır)
	 */
	private int createDepthBufferAttachment(int width, int height) {
		int depthBuffer = GL30.glGenRenderbuffers();
		GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthBuffer);
		GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL11.GL_DEPTH_COMPONENT, width,
				height);
		GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
				GL30.GL_RENDERBUFFER, depthBuffer);
		return depthBuffer;
	}

}
