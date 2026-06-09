package water.ocean;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import scene.Scene;
import utils.ICamera;
import water.tile.WaterFrameBuffers;

public class OceanRenderer {
	
	public static boolean renderWireframe = false;

	private OceanShader shader;
	private OceanMesh mesh;
	private OceanFFT fft;
	private WaterFrameBuffers fbos;

	private float time = 0;

	// Her bir parÃ§a (patch) performans ve draw call optimizasyonu iÃ§in daha geniÅŸ
	// bir alanÄ± kaplar.
	// scale = 20.0f yapÄ±ldÄ±ÄŸÄ±nda, 13x13 grid ile toplamda 5200x5200 birimlik devasa
	// bir okyanus oluÅŸturulur (Terrain ile tam uyumlu).
	private float scale = OceanFFT.PATCH_SIZE * 20.0f;
	private Vector3f oceanColor = new Vector3f(0.0001f, 0.0f, 0.0f);

	private boolean infiniteOcean = true;
	private Vector2f fixedCenter = new Vector2f(0, 0);

	// Preallocated fields to avoid 845 allocations per frame (GC Optimization)
	private final Matrix4f reusableWorldMatrix = new Matrix4f();
	private final Matrix4f reusableLocalMatrix = new Matrix4f();
	private final Vector3f reusableTranslation = new Vector3f();
	private final Vector3f xAxisRotation = new Vector3f(1, 0, 0);
	private final Vector3f scaleVector = new Vector3f();
	private final utils.Frustum frustum = new utils.Frustum();

	private int perlinTextureId = 0;
		private float renderDistanceScale = 1.0f;

	public void setRenderDistanceScale(float scale) {
		this.renderDistanceScale = scale;
	}

	public void setInfiniteOcean(boolean infinite) {
		this.infiniteOcean = infinite;
	}

	public void setFixedCenter(float x, float z) {
		this.fixedCenter.set(x, z);
	}

	public OceanRenderer(WaterFrameBuffers fbos) {
		this.shader = new OceanShader();
		this.mesh = new OceanMesh(128); // VarsayÄ±lanÄ± 64'ten 128'e Ã§Ä±kardÄ±k (daha kaliteli)
		this.fft = new OceanFFT();
		this.fbos = fbos;
		try {
			this.perlinTextureId = textures.Texture.newTexture(new utils.MyFile("res/perlin_noise.png")).anisotropic()
					.create().textureId;
		} catch (Exception e) {
			System.err.println("[OceanRenderer] Failed to load Perlin noise texture: " + e.getMessage());
		}
		// uvParams are now loaded dynamically per-patch in renderPatch
	}

	/** Oyun (First Person) modunda okyanusun mesh detayÄ±nÄ± kalÄ±cÄ± olarak yÃ¼kseltir */
	public void setHighQuality(boolean highQuality) {
		if (this.mesh != null) {
			this.mesh.cleanUp();
		}
		int newSize = highQuality ? 256 : 128; 
		this.mesh = new OceanMesh(newSize);
		System.out.println("[OceanRenderer] Quality updated. Mesh size: " + newSize + "x" + newSize);
	}

	public void render(Scene scene, ICamera camera, float delta) {
		time += delta;

		// Update wave simulation
		fft.update(time);

		prepareRender(camera, scene);

		// Update frustum
		frustum.update(camera.getProjectionViewMatrix());

		// Snap ocean grid to camera position to create infinite ocean illusion (or stay
		// fixed)
		float centerX = infiniteOcean ? camera.getPosition().x : fixedCenter.x;
		float centerZ = infiniteOcean ? camera.getPosition().z : fixedCenter.y;

		int gridX = (int) Math.floor(centerX / scale);
		int gridZ = (int) Math.floor(centerZ / scale);

		float patchRadius = scale * 1.5f;

		// Dynamically scale ocean patch range based on renderDistanceScale
		int range = Math.max(2, Math.round(4 * renderDistanceScale));
		for (int i = -range; i <= range; i++) {
			for (int j = -range; j <= range; j++) {
				float px = (gridX + i) * scale;
				float pz = (gridZ + j) * scale;
				
				if (scene.isFrustumCullingEnabled()) {
					float halfScale = scale * 0.5f;
					float minX = px - halfScale;
					float maxX = px + halfScale;
					float minY = -30.0f;
					float maxY = 30.0f;
					float minZ = pz - halfScale;
					float maxZ = pz + halfScale;
					
					if (!frustum.isBoxInside(minX, minY, minZ, maxX, maxY, maxZ)) {
						continue;
					}
				}
				
				renderPatch(px, 0, pz);
			}
		}

		unbind();
	}

	private void renderPatch(float x, float y, float z) {
		reusableTranslation.set(x, y, z);

		// World matrix setup without allocation
		reusableWorldMatrix.setIdentity();
		Matrix4f.translate(reusableTranslation, reusableWorldMatrix, reusableWorldMatrix);
		Matrix4f.rotate((float) Math.toRadians(90), xAxisRotation, reusableWorldMatrix, reusableWorldMatrix);
		shader.loadMatWorld(reusableWorldMatrix);

		// Local matrix setup without allocation
		reusableLocalMatrix.setIdentity();
		scaleVector.set(scale, scale, scale);
		Matrix4f.scale(scaleVector, reusableLocalMatrix, reusableLocalMatrix);
		shader.loadMatLocal(reusableLocalMatrix);

		// Dynamic uvParams per-patch for continuous world space Perlin noise
		// coordinates
		shader.loadUVParams(new Vector4f(
				1.0f / OceanFFT.PATCH_SIZE,
				0.5f / OceanFFT.DISP_MAP_SIZE,
				x / OceanFFT.PATCH_SIZE,
				z / OceanFFT.PATCH_SIZE));
				
		if (renderWireframe) {
			GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
			// Tel kafes çok karanlık olmasın diye okyanus rengini geçici olarak parlak yapalım
			shader.loadOceanColor(new Vector3f(0.0f, 1.0f, 1.0f)); 
		}

		GL11.glDrawElements(GL11.GL_TRIANGLES, mesh.getVao().getIndexCount(), GL11.GL_UNSIGNED_INT, 0);
		
		if (renderWireframe) {
			GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
			shader.loadOceanColor(oceanColor); // Rengi eski haline döndür
		}
	}

	private void prepareRender(ICamera camera, Scene scene) {
		shader.start();
		shader.loadMatViewProj(camera);
		shader.loadOceanColor(oceanColor);

		// Load animated Perlin waves offset in direction of wind
		Vector2f windDir = new Vector2f(-0.4f, -0.9f);
		shader.loadPerlinOffset(new Vector2f(windDir.x * time * 0.06f, windDir.y * time * 0.06f));

		// Load Scene Lighting
		shader.loadLighting(scene.getLightDirection(), scene.getLightColor(),
				new Vector3f(scene.getAmbientLight(), scene.getAmbientLight(), scene.getAmbientLight()));

		shader.loadFogParams(scene.getFogColor(), scene.getFogDensity(), scene.getFogStart());

		// Rüzgar şiddetine göre dalga boyutlarını belirle
		float windSpeed = scene.getWindVelocity().length();
		float windStrength = Math.max(0.2f, windSpeed * 0.15f); // Dalga yüksekliği rüzgarla artsın
		shader.loadWindStrength(windStrength);

		// Load Cloud Shadows
		if (scene.getSky() != null && scene.getSky() instanceof skybox.atmosphere.AtmosphereSky) {
			skybox.atmosphere.AtmosphereSky sky = (skybox.atmosphere.AtmosphereSky) scene.getSky();
			shader.loadCloudShadowData(sky.getTime(),
					new org.lwjgl.util.vector.Vector2f(scene.getWindVelocity().x, scene.getWindVelocity().z),
					sky.isCloudsEnabled(), sky.getClusters());
		} else {
			shader.loadCloudShadowData(0f, new org.lwjgl.util.vector.Vector2f(0, 0), false, null);
		}
		
		// Gemi içi su kesme (Cutout)
		boolean shipFound = false;
		for (scene.Entity e : scene.getAllEntities()) {
			if (e instanceof scene.GameObject) {
				scene.GameObject go = (scene.GameObject) e;
				if (go.getComponent(default_controls.ShipController.class) != null) {
					// Gemi bulundu, verileri gönder
					// Genişlik ve Uzunluk tahmini değerler, gemi modeline göre ayarlanabilir
					org.lwjgl.util.vector.Vector2f shipDim = new org.lwjgl.util.vector.Vector2f(7.5f, 2.8f); 
					// Geçici olarak "false" yapıyoruz çünkü geminin tabanı yoksa denizin dibi görünüyor (boşluk hatası)
					shader.loadShipCutout(go.getPosition(), shipDim, (float) Math.toRadians(go.getRotation().y), false);
					shipFound = true;
					break;
				}
			}
		}
		if (!shipFound) {
			shader.loadShipCutout(new Vector3f(), new org.lwjgl.util.vector.Vector2f(), 0, false);
		}

		mesh.getVao().bind(0);

		// Bind FFT generated maps
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, fft.getDisplacementTexture());
		GL13.glActiveTexture(GL13.GL_TEXTURE1);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, perlinTextureId); // Bind Perlin noise texture
		GL13.glActiveTexture(GL13.GL_TEXTURE3);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, fft.getGradientsTexture());

		// Bind WaterFrameBuffers for Planar Reflections & Refractions
		GL13.glActiveTexture(GL13.GL_TEXTURE4);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbos.getReflectionTexture());
		GL13.glActiveTexture(GL13.GL_TEXTURE5);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbos.getRefractionTexture());
		GL13.glActiveTexture(GL13.GL_TEXTURE6);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbos.getRefractionDepthTexture());

		shader.loadDepthParams(0.1f, 1000.0f);

		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	}

	private void unbind() {
		mesh.getVao().unbind(0);

		// Unbind textures
		for (int i = 0; i <= 6; i++) {
			GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		}

		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_CULL_FACE); // Re-enable if engine uses it
		shader.stop();
	}

	public void cleanUp() {
		shader.cleanUp();
		mesh.cleanUp();
		fft.cleanUp();
		if (perlinTextureId != 0) {
			GL11.glDeleteTextures(perlinTextureId);
		}
	}
}

