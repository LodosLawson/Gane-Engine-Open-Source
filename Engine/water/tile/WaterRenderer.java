package water.tile;

import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import openglObjects.Vao;
import textures.Texture;
import utils.ICamera;
import utils.MyFile;
import utils.OpenGlUtils;
import water.tile.QuadGenerator;
import water.tile.WaterFrameBuffers;

public class WaterRenderer {

	private static final MyFile NORMAL_MAP = new MyFile("res", "normal.png");

	private Vao quad;
	private WaterShader shader;
	private WaterFrameBuffers fbos;

	private Texture normalMap;

	public WaterRenderer(WaterFrameBuffers fbos) {
		this.shader = new WaterShader();
		this.fbos = fbos;
		this.quad = QuadGenerator.generateQuad();
		this.normalMap = Texture.newTexture(NORMAL_MAP).create();
	}

	public void render(List<WaterTile> waterTiles, ICamera camera, Vector3f lightDir, float delta) {
		if (waterTiles == null || waterTiles.isEmpty()) {
			return;
		}

		System.out.println("DEBUG: Rendering water with " + waterTiles.size() + " tiles.");
		prepareRender(camera, lightDir);

		for (WaterTile tile : waterTiles) {
			tile.update(delta);
			
			Matrix4f modelMatrix = createModelMatrix(tile.getX(), tile.getHeight(), tile.getZ(), tile.getSize());
			shader.loadModelMatrix(modelMatrix);

			shader.loadWaterParameters(tile.getTime(), tile.getBaseColor(), tile.getTransparency(), tile.getTextureScale());

			GL11.glDrawElements(GL11.GL_TRIANGLES, quad.getIndexCount(), GL11.GL_UNSIGNED_INT, 0);
		}
		
		finish();
	}

	public void cleanUp() {
		quad.delete();
		normalMap.delete();
		fbos.cleanUp();
		shader.cleanUp();
	}

	private void prepareRender(ICamera camera, Vector3f lightDir) {
		shader.start();
		shader.loadProjectionMatrix(camera.getProjectionMatrix());
		shader.loadViewMatrix(camera);
		
		// Load a default sun color and ambient from the first tile (assuming consistent lighting for now)
		// Or hardcode reasonable defaults resembling the Asylum_Tutorials scene
		shader.loadLighting(lightDir, new Vector3f(1.0f, 1.0f, 1.0f), new Vector3f(0.015f, 0.015f, 0.015f));
		
		shader.loadDepthPlanes(0.1f, 10000f);

		quad.bind(0);
		shader.connectTextureUnits();
		bindTextures();
		doRenderSettings();
	}

	private void bindTextures() {
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbos.getReflectionTexture());

		GL13.glActiveTexture(GL13.GL_TEXTURE1);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbos.getRefractionTexture());

		normalMap.bindToUnit(2);
		
		GL13.glActiveTexture(GL13.GL_TEXTURE3);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbos.getRefractionDepthTexture());
	}

	private void doRenderSettings() {
		OpenGlUtils.enableDepthTesting(true);
		OpenGlUtils.antialias(false);
		OpenGlUtils.cullBackFaces(false); // Render from below water too
		OpenGlUtils.enableAlphaBlending();
	}

	private void finish() {
		quad.unbind(0);
		shader.stop();
		for (int i = 0; i < 4; i++) {
			GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		}
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
	}

	private Matrix4f createModelMatrix(float x, float y, float z, float scale) {
		Matrix4f modelMatrix = new Matrix4f();
		Matrix4f.translate(new Vector3f(x, y, z), modelMatrix, modelMatrix);
		Matrix4f.scale(new Vector3f(scale, scale, scale), modelMatrix, modelMatrix);
		return modelMatrix;
	}
}


