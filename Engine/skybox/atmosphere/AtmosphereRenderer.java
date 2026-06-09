package skybox.atmosphere;

import skybox.SphereGenerator;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;
import org.lwjgl.BufferUtils;
import java.nio.FloatBuffer;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import openglObjects.Vao;
import openglObjects.Vbo;
import utils.ICamera;
import utils.OpenGlUtils;

/**
 * AtmosphereSky objesini ekrana cizen renderer sinifi.
 * Fizik tabanlÃ„Â± bulut kÃƒÂ¼meleri desteklenir.
 */
public class AtmosphereRenderer {

	private static final float PLANET_RADIUS = 5000f;
	private static final float SUN_RADIUS = 0.001f;
	private static final int MAX_PARTICLES = 10000;
	private static final int INSTANCE_DATA_LENGTH = 18; // 16 for matrix + 2 for alpha/density

	private Vao sphere;
	private Vao cloudSphere;
	private Vbo vbo;
	private AtmosphereShader shader;
	private FloatBuffer buffer = BufferUtils.createFloatBuffer(MAX_PARTICLES * INSTANCE_DATA_LENGTH);

	// Delta time takibi (update icin)
	private long lastTimeMs = System.currentTimeMillis();

	// Preallocated fields for rendering optimization
	private final Vector3f planetCenter = new Vector3f();
	private final Vector3f sunPos = new Vector3f();
	private final Vector3f reusableVector3f = new Vector3f();
	private final Matrix4f starMatrix = new Matrix4f();
	private final Matrix4f sunModelMatrix = new Matrix4f();
	private final Matrix4f planetModelMatrix = new Matrix4f();
	private final Matrix4f cloudMatrix = new Matrix4f();

	public AtmosphereRenderer() {
		this.sphere = SphereGenerator.generateSphere(1f, 60, 60);
		// Bulutlar icin cok dusuk poligonlu kure (Instanced rendering'de FPS dusmemesi icin)
		this.cloudSphere = SphereGenerator.generateSphere(1f, 10, 10);
		this.shader = new AtmosphereShader();
		
		this.vbo = Vbo.create(GL15.GL_ARRAY_BUFFER);
		this.vbo.bind();
		this.vbo.allocateData(MAX_PARTICLES * INSTANCE_DATA_LENGTH);
		this.vbo.unbind();
		
		cloudSphere.bind();
		vbo.bind();
		cloudSphere.addInstancedAttribute(cloudSphere.id, vbo.getId(), 1, 4, INSTANCE_DATA_LENGTH, 0);
		cloudSphere.addInstancedAttribute(cloudSphere.id, vbo.getId(), 2, 4, INSTANCE_DATA_LENGTH, 4);
		cloudSphere.addInstancedAttribute(cloudSphere.id, vbo.getId(), 3, 4, INSTANCE_DATA_LENGTH, 8);
		cloudSphere.addInstancedAttribute(cloudSphere.id, vbo.getId(), 4, 4, INSTANCE_DATA_LENGTH, 12);
		cloudSphere.addInstancedAttribute(cloudSphere.id, vbo.getId(), 5, 2, INSTANCE_DATA_LENGTH, 16);
		vbo.unbind();
		cloudSphere.unbind();
	}

	public void render(AtmosphereSky sky, ICamera camera, Vector3f lightDir) {
		// Delta hesapla
		long now = System.currentTimeMillis();
		float delta = Math.min((now - lastTimeMs) / 1000f, 0.1f);
		lastTimeMs = now;

		// Keyboard.KEY_T basili ise hizi 25 katina cikar
		boolean speedUp = false;
		try {
			if (org.lwjgl.input.Keyboard.isCreated()
					&& org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_T)) {
				speedUp = true;
			}
		} catch (Exception e) {
			// ignore
		}
		if (speedUp) {
			delta *= 25.0f;
		}

		// Fiziksel kararlilik icin alt-adimlama (sub-stepping)
		float totalDelta = delta;
		float stepSize = 0.05f;
		int steps = (int) Math.ceil(totalDelta / stepSize);
		if (steps > 0) {
			float subDelta = totalDelta / steps;
			for (int i = 0; i < steps; i++) {
				sky.update(subDelta, camera.getPosition());
			}
		} else {
			sky.update(0f, camera.getPosition());
		}

		shader.start();

		GL11.glDepthMask(true);

		shader.projectionViewMatrix.loadMatrix(camera.getProjectionViewMatrix());
		shader.cameraPosition.loadVec3(camera.getPosition());

		planetCenter.set(camera.getPosition().x, 0.0f, camera.getPosition().z);
		if (sky.isPlanetaryMode()) {
			planetCenter.set(0.0f, 0.0f, 0.0f);
		}
		shader.planetCenter.loadVec3(planetCenter);

		sunPos.set(-lightDir.x, -lightDir.y, -lightDir.z);
		if (sunPos.lengthSquared() > 0)
			sunPos.normalise();
		else
			sunPos.set(0, 1, 0);

		float sunDistance = sky.isPlanetaryMode() ? 800f : 25000f;
		sunPos.x = planetCenter.x + sunPos.x * sunDistance;
		sunPos.y = planetCenter.y + sunPos.y * sunDistance;
		sunPos.z = planetCenter.z + sunPos.z * sunDistance;

		shader.sunPosition.loadVec3(sunPos);
		shader.atmosphereThickness.loadFloat(sky.getAtmosphereThickness());
		shader.skyColorDay.loadVec3(sky.getSkyColorDay());
		shader.skyColorSunset.loadVec3(sky.getSkyColorSunset());
		shader.spaceColor.loadVec3(sky.getSpaceColor());
		shader.time.loadFloat(sky.getTime());
		shader.uFogDensity.loadFloat(sky.getFogDensity());
		shader.uFogColor.loadVec3(sky.getFogColor());

		GL11.glClearColor(sky.getSpaceColor().x, sky.getSpaceColor().y, sky.getSpaceColor().z, 1.0f);

		OpenGlUtils.enableDepthTesting(true);
		GL11.glDepthFunc(GL11.GL_LEQUAL);

		float distToCenter = Math.abs(camera.getPosition().y);
		if (distToCenter < PLANET_RADIUS) {
			OpenGlUtils.cullBackFaces(false);
		} else {
			OpenGlUtils.cullBackFaces(true);
		}

		OpenGlUtils.enableAlphaBlending();
		OpenGlUtils.antialias(false);

		sphere.bind(0);

		// Yildiz kuresi
		OpenGlUtils.cullBackFaces(false);
		GL11.glDepthMask(false);
		shader.isSun.loadFloat(3.0f);
		starMatrix.setIdentity();
		Matrix4f.translate(camera.getPosition(), starMatrix, starMatrix);
		reusableVector3f.set(40000f, 40000f, 40000f);
		Matrix4f.scale(reusableVector3f, starMatrix, starMatrix);
		shader.modelMatrix.loadMatrix(starMatrix);
		GL11.glDrawElements(GL11.GL_TRIANGLES, sphere.getIndexCount(), GL11.GL_UNSIGNED_INT, 0);

		// Derinlik yazmayi (depth mask) Gunes, Gezegen ve Bulutlar icin kapali tutmaya
		// devam et.
		// Boylece ayni uzaklikta / daha uzakta olsalar bile siralama ile ust uste
		// (overdraw) dogru cizilirler.
		// (Fakat Depth Test acik oldugu icin Terrain'in arkasinda kalirlar, ki bu
		// dogru).

		if (distToCenter < PLANET_RADIUS) {
			OpenGlUtils.cullBackFaces(false);
		} else {
			OpenGlUtils.cullBackFaces(true);
		}

		// Gunes
		shader.isSun.loadFloat(1.0f);
		sunModelMatrix.setIdentity();
		Matrix4f.translate(sunPos, sunModelMatrix, sunModelMatrix);
		reusableVector3f.set(SUN_RADIUS, SUN_RADIUS, SUN_RADIUS);
		Matrix4f.scale(reusableVector3f, sunModelMatrix, sunModelMatrix);
		shader.modelMatrix.loadMatrix(sunModelMatrix);
		GL11.glDrawElements(GL11.GL_TRIANGLES, sphere.getIndexCount(), GL11.GL_UNSIGNED_INT, 0);

		// Gezegen / Atmosfer
		float currentPlanetRadius = sky.isPlanetaryMode() ? 64f : PLANET_RADIUS;

		if (distToCenter < currentPlanetRadius) {
			drawPlanet(shader, planetCenter, currentPlanetRadius);
			sphere.unbind(0);
			drawClusters(sky, shader, planetCenter);
			sphere.bind(0);
		} else {
			sphere.unbind(0);
			drawClusters(sky, shader, planetCenter);
			sphere.bind(0);
			drawPlanet(shader, planetCenter, currentPlanetRadius);
		}

		sphere.unbind(0);
		OpenGlUtils.disableBlending();

		// RENDER SONRASI DERINLIK MASKESINI GERI ACMAK ZORUNLUDUR!
		// Eger kapali kalirsa, MasterRenderer bir sonraki frame'de
		// glClear(GL_DEPTH_BUFFER_BIT) yapamaz ve tum ekran siyah/bozuk cikar!
		GL11.glDepthMask(true);
		GL11.glDepthFunc(GL11.GL_LESS);

		shader.stop();
	}

	private void drawPlanet(AtmosphereShader shader, Vector3f planetCenter, float radius) {
		shader.isSun.loadFloat(0.0f);
		planetModelMatrix.setIdentity();
		Matrix4f.translate(planetCenter, planetModelMatrix, planetModelMatrix);
		reusableVector3f.set(radius, radius, radius);
		Matrix4f.scale(reusableVector3f, planetModelMatrix, planetModelMatrix);
		shader.modelMatrix.loadMatrix(planetModelMatrix);
		GL11.glDrawElements(GL11.GL_TRIANGLES, sphere.getIndexCount(), GL11.GL_UNSIGNED_INT, 0);
	}

	/**
	 * CloudCluster listesindeki her kume icin parcaciklari cizer.
	 * Instanced Rendering kullanarak binlerce parcacigi tek bir draw call ile gonderir.
	 */
	private void drawClusters(AtmosphereSky sky, AtmosphereShader shader, Vector3f planetCenter) {
		if (!sky.isCloudsEnabled())
			return;

		shader.isSun.loadFloat(2.0f);
		buffer.clear();

		int particleCount = 0;

		for (CloudCluster cluster : sky.getClusters()) {
			float scaleFactor = cluster.currentScale / cluster.scale;

			for (org.lwjgl.util.vector.Vector4f p : cluster.particles) {
				if (particleCount >= MAX_PARTICLES) break;
				
				float wx = cluster.position.x + p.x * scaleFactor;
				float wy = cluster.position.y + p.y * scaleFactor;
				float wz = cluster.position.z + p.z * scaleFactor;
				float r = p.w * scaleFactor;

				cloudMatrix.setIdentity();
				reusableVector3f.set(wx, wy, wz);
				Matrix4f.translate(reusableVector3f, cloudMatrix, cloudMatrix);
				reusableVector3f.set(r, r, r);
				Matrix4f.scale(reusableVector3f, cloudMatrix, cloudMatrix);
				
				// Matrix (16 floats)
				buffer.put(cloudMatrix.m00);
				buffer.put(cloudMatrix.m01);
				buffer.put(cloudMatrix.m02);
				buffer.put(cloudMatrix.m03);
				buffer.put(cloudMatrix.m10);
				buffer.put(cloudMatrix.m11);
				buffer.put(cloudMatrix.m12);
				buffer.put(cloudMatrix.m13);
				buffer.put(cloudMatrix.m20);
				buffer.put(cloudMatrix.m21);
				buffer.put(cloudMatrix.m22);
				buffer.put(cloudMatrix.m23);
				buffer.put(cloudMatrix.m30);
				buffer.put(cloudMatrix.m31);
				buffer.put(cloudMatrix.m32);
				buffer.put(cloudMatrix.m33);
				
				// Properties (2 floats)
				buffer.put(cluster.alpha);
				buffer.put(cluster.density);
				
				particleCount++;
			}
		}

		if (particleCount > 0) {
			buffer.flip();
			vbo.bind();
			vbo.updateData(buffer);
			vbo.unbind();
			
			cloudSphere.bind(0);
			
			org.lwjgl.opengl.GL20.glEnableVertexAttribArray(1);
			org.lwjgl.opengl.GL20.glEnableVertexAttribArray(2);
			org.lwjgl.opengl.GL20.glEnableVertexAttribArray(3);
			org.lwjgl.opengl.GL20.glEnableVertexAttribArray(4);
			org.lwjgl.opengl.GL20.glEnableVertexAttribArray(5);
			
			cloudSphere.bindIndexBuffer();
			GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, cloudSphere.getIndexCount(), GL11.GL_UNSIGNED_INT, 0, particleCount);
			
			org.lwjgl.opengl.GL20.glDisableVertexAttribArray(1);
			org.lwjgl.opengl.GL20.glDisableVertexAttribArray(2);
			org.lwjgl.opengl.GL20.glDisableVertexAttribArray(3);
			org.lwjgl.opengl.GL20.glDisableVertexAttribArray(4);
			org.lwjgl.opengl.GL20.glDisableVertexAttribArray(5);
			
			cloudSphere.unbind(0);
		}
	}

	public void cleanUp() {
		shader.cleanUp();
		if (vbo != null) {
			vbo.delete();
		}
	}

}


