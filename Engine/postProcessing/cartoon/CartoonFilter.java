package postProcessing.cartoon;

import postProcessing.ImageRenderer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class CartoonFilter {

	private ImageRenderer renderer;
	private CartoonShader shader;

	public CartoonFilter(int width, int height) {
		shader = new CartoonShader();
		shader.start();
		shader.loadScreenSize(width, height);
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

	public void cleanUp() {
		renderer.cleanUp();
		shader.cleanUp();
	}
}
