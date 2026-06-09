package postProcessing.grayscale;

import postProcessing.ImageRenderer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class GrayscaleFilter {

	private ImageRenderer renderer;
	private GrayscaleShader shader;

	public GrayscaleFilter() {
		shader = new GrayscaleShader();
		renderer = new ImageRenderer();
	}

	public void render(int texture) {
		shader.start();
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
		renderer.renderQuad();
		shader.stop();
	}

	public void cleanUp() {
		renderer.cleanUp();
		shader.cleanUp();
	}
}
