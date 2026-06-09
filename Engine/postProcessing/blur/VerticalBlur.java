package postProcessing.blur;

import postProcessing.ImageRenderer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class VerticalBlur {

	private ImageRenderer renderer;
	private VerticalBlurShader shader;

	public VerticalBlur(int targetFboWidth, int targetFboHeight) {
		shader = new VerticalBlurShader();
		shader.start();
		shader.loadTargetHeight(targetFboHeight);
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


