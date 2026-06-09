package postProcessing.pixelate;

import postProcessing.ImageRenderer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class PixelateFilter {

	private ImageRenderer renderer;
	private PixelateShader shader;

	public PixelateFilter(int width, int height) {
		shader = new PixelateShader();
		shader.start();
		shader.loadScreenSize(width, height);
		shader.loadPixelSize(10.0f); // Varsayılan piksel boyutu
		shader.stop();
		renderer = new ImageRenderer();
	}

	public void render(int texture) {
		shader.start();
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
		renderer.renderQuad();
		shader.stop();
	}
	
	public void setPixelSize(float size) {
		shader.start();
		shader.loadPixelSize(size);
		shader.stop();
	}

	public void cleanUp() {
		renderer.cleanUp();
		shader.cleanUp();
	}

}
