package postProcessing;

import org.lwjgl.opengl.GL11;

import openglObjects.Vao;

public class ImageRenderer {

	private Vao fboQuad;

	public ImageRenderer() {
		this.fboQuad = Vao.create();
		fboQuad.bind();
		fboQuad.storeData(4, new float[]{-1, 1, -1, -1, 1, 1, 1, -1});
		fboQuad.unbind();
	}

	public void renderQuad() {
		fboQuad.bind(0);
		GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
		fboQuad.unbind(0);
	}

	public void cleanUp() {
		fboQuad.delete();
	}

}
