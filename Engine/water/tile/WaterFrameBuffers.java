package water.tile;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;

/**
 * Su efekti iÃ§in gerekli olan yansÄ±ma (Reflection) ve kÄ±rÄ±lma (Refraction)
 * Frame Buffer Object (FBO) iÅŸlemlerini yÃ¶netir.
 * KameranÄ±n konumuna gÃ¶re ayrÄ± sahneleri renderlayÄ±p bu dokularÄ± suya uygular.
 */
public class WaterFrameBuffers {

	// YansÄ±ma dokusunun Ã§Ã¶zÃ¼nÃ¼rlÃ¼ÄŸÃ¼
	protected static final int REFLECTION_WIDTH = 640;
	private static final int REFLECTION_HEIGHT = 360;
	
	// KÄ±rÄ±lma dokusunun Ã§Ã¶zÃ¼nÃ¼rlÃ¼ÄŸÃ¼
	protected static final int REFRACTION_WIDTH = 1280;
	private static final int REFRACTION_HEIGHT = 720;

	private int reflectionFrameBuffer;
	private int reflectionTexture;
	private int reflectionDepthBuffer; // Derinlik tamponu (Renderbuffer)
	
	private int refractionFrameBuffer;
	private int refractionTexture;
	private int refractionDepthTexture; // Derinlik dokusu (Su derinliÄŸini hesaplamak iÃ§in)

	/**
	 * Oyun yÃ¼klenirken Ã§aÄŸrÄ±lÄ±r. Gerekli FBO'larÄ± oluÅŸturur.
	 */
	public WaterFrameBuffers() {
		initialiseReflectionFrameBuffer();
		initialiseRefractionFrameBuffer();
	}

	/**
	 * Oyun kapanÄ±rken Ã§aÄŸrÄ±lÄ±r. OpenGL belleÄŸini temizler.
	 */
	public void cleanUp() {
		GL30.glDeleteFramebuffers(reflectionFrameBuffer);
		GL11.glDeleteTextures(reflectionTexture);
		GL30.glDeleteRenderbuffers(reflectionDepthBuffer);
		GL30.glDeleteFramebuffers(refractionFrameBuffer);
		GL11.glDeleteTextures(refractionTexture);
		GL11.glDeleteTextures(refractionDepthTexture);
	}

	/** YansÄ±ma Ã§izimi yapÄ±lmadan Ã¶nce Ã§aÄŸrÄ±lÄ±r. */
	public void bindReflectionFrameBuffer() {
		bindFrameBuffer(reflectionFrameBuffer, REFLECTION_WIDTH, REFLECTION_HEIGHT);
	}
	
	/** KÄ±rÄ±lma Ã§izimi yapÄ±lmadan Ã¶nce Ã§aÄŸrÄ±lÄ±r. */
	public void bindRefractionFrameBuffer() {
		bindFrameBuffer(refractionFrameBuffer, REFRACTION_WIDTH, REFRACTION_HEIGHT);
	}
	
	/**
	 * FBO'ya render iÅŸlemi bittikten sonra Ã§aÄŸrÄ±lÄ±r ve normal ekrana (default FBO) geri dÃ¶nÃ¼lÃ¼r.
	 */
	public void unbindCurrentFrameBuffer() {
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0); // 0, varsayÄ±lan ekrandÄ±r
		GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
		GL11.glFinish();
	}

	/** @return YansÄ±ma dokusunun (texture) ID'si */
	public int getReflectionTexture() {
		return reflectionTexture;
	}
	
	/** @return KÄ±rÄ±lma dokusunun (texture) ID'si */
	public int getRefractionTexture() {
		return refractionTexture;
	}
	
	/** @return KÄ±rÄ±lma iÅŸlemi iÃ§in oluÅŸturulan derinlik dokusunun ID'si */
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
		// KÄ±rÄ±lma iÃ§in su derinliÄŸini (suyun altÄ±ndaki nesnenin mesafesi) bilmemiz gerektiÄŸinden derinlik dokusu kullanÄ±rÄ±z
		refractionDepthTexture = createDepthTextureAttachment(REFRACTION_WIDTH, REFRACTION_HEIGHT);
		unbindCurrentFrameBuffer();
	}
	
	/**
	 * Ä°lgili FBO'yu aktif hale getirir ve gÃ¶rÃ¼nÃ¼m alanÄ±nÄ± (viewport) ayarlar.
	 */
	private void bindFrameBuffer(int frameBuffer, int width, int height){
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0); // DokularÄ±n baÄŸlantÄ±sÄ±nÄ± kes
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, frameBuffer);
		GL11.glViewport(0, 0, width, height); // FBO Ã§Ã¶zÃ¼nÃ¼rlÃ¼ÄŸÃ¼ne gÃ¶re ayarla
	}

	/**
	 * Yeni bir Frame Buffer Object (FBO) oluÅŸturur.
	 */
	private int createFrameBuffer() {
		int frameBuffer = GL30.glGenFramebuffers();
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, frameBuffer);
		// Renderlanan rengin 0 numaralÄ± attachment'a (baÄŸlantÄ± noktasÄ±na) gideceÄŸini belirtiyoruz
		GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
		GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
		return frameBuffer;
	}

	/**
	 * FBO iÃ§in renk dokusu eklentisi (Color Attachment) oluÅŸturur.
	 */
	private int createTextureAttachment(int width, int height) {
		int texture = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
		// BoÅŸ bir doku oluÅŸtur
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, width, height,
				0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		// Dokuyu FBO'nun renk eklentisine baÄŸla
		GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
				texture, 0);
		return texture;
	}
	
	/**
	 * FBO iÃ§in derinlik dokusu eklentisi (Depth Texture Attachment) oluÅŸturur.
	 * Ekrandaki nesnelerin su yÃ¼zeyine olan derinliÄŸini okumak iÃ§in kullanÄ±lÄ±r.
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
	 * Sadece derinlik testi (Depth Test) yapmak iÃ§in bir Renderbuffer eklentisi oluÅŸturur.
	 * (Dokudan okuma gerekmeyen durumlarda performansÄ± artÄ±rÄ±r)
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

