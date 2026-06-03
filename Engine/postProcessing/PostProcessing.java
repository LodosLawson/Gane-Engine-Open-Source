package postProcessing;

import postProcessing.blur.HorizontalBlur;
import postProcessing.blur.VerticalBlur;
import postProcessing.contrast.ContrastFilter;
import postProcessing.contrast.CombineFilter;
import postProcessing.pixelate.PixelateFilter;
import postProcessing.grayscale.GrayscaleFilter;
import postProcessing.cartoon.CartoonFilter;


import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import utils.MyFile;

public class PostProcessing {
	
	private static final float[] POSITIONS = { -1, 1, -1, -1, 1, 1, 1, -1 };	
	private static openglObjects.Vao quad;
	private static ContrastFilter contrastFilter;
	private static HorizontalBlur hBlur;
	private static VerticalBlur vBlur;
	private static CombineFilter combineFilter;
	private static Fbo contrastFbo;
	private static Fbo hBlurFbo;
	private static Fbo vBlurFbo;
	
	public enum RenderMode { NORMAL, PIXELATE, GRAYSCALE, CARTOON }
	public static RenderMode currentRenderMode = RenderMode.NORMAL;
	
	private static PixelateFilter pixelateFilter;
	private static GrayscaleFilter grayscaleFilter;
	private static CartoonFilter cartoonFilter;
	
	private static Fbo effectFbo;

	public static void init() {
		quad = openglObjects.Vao.create();
		quad.bind();
		quad.storeData(4, POSITIONS);
		quad.unbind();
		
		contrastFilter = new ContrastFilter();
		hBlur = new HorizontalBlur(org.lwjgl.opengl.Display.getWidth() / 4, org.lwjgl.opengl.Display.getHeight() / 4);
		vBlur = new VerticalBlur(org.lwjgl.opengl.Display.getWidth() / 4, org.lwjgl.opengl.Display.getHeight() / 4);
		combineFilter = new CombineFilter();
		pixelateFilter = new PixelateFilter(org.lwjgl.opengl.Display.getWidth(), org.lwjgl.opengl.Display.getHeight());
		grayscaleFilter = new GrayscaleFilter();
		cartoonFilter = new CartoonFilter(org.lwjgl.opengl.Display.getWidth(), org.lwjgl.opengl.Display.getHeight());
		
		contrastFbo = new Fbo(org.lwjgl.opengl.Display.getWidth(), org.lwjgl.opengl.Display.getHeight(), Fbo.NONE);
		hBlurFbo = new Fbo(org.lwjgl.opengl.Display.getWidth() / 4, org.lwjgl.opengl.Display.getHeight() / 4, Fbo.NONE);
		vBlurFbo = new Fbo(org.lwjgl.opengl.Display.getWidth() / 4, org.lwjgl.opengl.Display.getHeight() / 4, Fbo.NONE);
		effectFbo = new Fbo(org.lwjgl.opengl.Display.getWidth(), org.lwjgl.opengl.Display.getHeight(), Fbo.NONE);
	}
	
	public static void doPostProcessing(int colourTexture){
		start();
		
		// 1. Ayrim Filtresi: Parlak yerleri bul
		contrastFbo.bindFrameBuffer();
		contrastFilter.render(colourTexture);
		contrastFbo.unbindFrameBuffer();
		
		// 2. Horizontal Blur
		hBlurFbo.bindFrameBuffer();
		hBlur.render(contrastFbo.getColourTexture());
		hBlurFbo.unbindFrameBuffer();
		
		// 3. Vertical Blur
		vBlurFbo.bindFrameBuffer();
		vBlur.render(hBlurFbo.getColourTexture());
		vBlurFbo.unbindFrameBuffer();
		
		// 4. Birlestirme: Ana ekranla bulanik ve parlayan kismi birlestir
		if (currentRenderMode != RenderMode.NORMAL) {
			effectFbo.bindFrameBuffer();
		}
		
		combineFilter.render(colourTexture, vBlurFbo.getColourTexture());
		
		if (currentRenderMode != RenderMode.NORMAL) {
			effectFbo.unbindFrameBuffer();
			switch (currentRenderMode) {
				case PIXELATE:
					pixelateFilter.render(effectFbo.getColourTexture());
					break;
				case GRAYSCALE:
					grayscaleFilter.render(effectFbo.getColourTexture());
					break;
				case CARTOON:
					cartoonFilter.render(effectFbo.getColourTexture());
					break;
				default:
					break;
			}
		}
		
		end();
	}
	
	public static void cleanUp(){
		contrastFilter.cleanUp();
		hBlur.cleanUp();
		vBlur.cleanUp();
		combineFilter.cleanUp();
		pixelateFilter.cleanUp();
		grayscaleFilter.cleanUp();
		cartoonFilter.cleanUp();
		contrastFbo.cleanUp();
		hBlurFbo.cleanUp();
		vBlurFbo.cleanUp();
		effectFbo.cleanUp();
		quad.delete();
	}
	
	private static void start(){
		GL30.glBindVertexArray(quad.id);
		GL20.glEnableVertexAttribArray(0);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
	}
	
	private static void end(){
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL20.glDisableVertexAttribArray(0);
		GL30.glBindVertexArray(0);
	}


}


