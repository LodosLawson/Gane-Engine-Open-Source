package particles;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector3f;

import scene.Scene;
import utils.ICamera;

/**
 * Sahnedeki tÃ¼m partikÃ¼llerin yaÅŸam dÃ¶ngÃ¼sÃ¼nÃ¼, spawn edilmesini ve ekrana Ã§izilmesini yÃ¶netir.
 * BaÅŸlangÄ±Ã§ta GPU belleÄŸinde prosedÃ¼rel bir partikÃ¼l dokusu (yumuÅŸak daire) oluÅŸturur.
 */
public class ParticleManager {

	private static ParticleManager instance;
	private final List<Particle> particles = new ArrayList<>();
	private int textureId = -1;

	public static ParticleManager getInstance() {
		if (instance == null) {
			instance = new ParticleManager();
		}
		return instance;
	}

	private ParticleManager() {
		// DÄ±ÅŸarÄ±dan dosya yÃ¼kleme gerekliliÄŸini sÄ±fÄ±rlamak iÃ§in Ã§alÄ±ÅŸma anÄ±nda yumuÅŸak daire dokusu Ã¼ret
		this.textureId = createProceduralParticleTexture();
	}

	/**
	 * TÃ¼m aktif parÃ§acÄ±klarÄ± gÃ¼nceller.
	 * @param scene Dalga yÃ¼ksekliÄŸi referanslarÄ± iÃ§in sahne nesnesi
	 * @param delta Son kareden beri geÃ§en sÃ¼re
	 */
	public void update(Scene scene, float delta) {
		if (particles.isEmpty()) return;

		float baseWaterHeight = -2.0f;
		boolean hasWater = scene != null && !scene.getWater().isEmpty();
		water.tile.WaterTile waterTile = null;
		
		if (hasWater) {
			waterTile = scene.getWater().get(0);
			baseWaterHeight = waterTile.getHeight();
		}

		Iterator<Particle> it = particles.iterator();
		while (it.hasNext()) {
			Particle p = it.next();
			float currentWaterHeight = baseWaterHeight;
			
			// Her partikÃ¼lÃ¼n tam X/Z koordinatÄ±ndaki dalgalÄ± su yÃ¼ksekliÄŸini al
			if (hasWater) {
				currentWaterHeight = waterTile.getWaterHeightAt(p.position.x, p.position.z);
			}
			
			if (!p.update(delta, currentWaterHeight)) {
				it.remove();
			}
		}
	}

	/**
	 * Billboard tekniÄŸini (kameraya bakÄ±ÅŸ) kullanarak tÃ¼m partikÃ¼lleri tek bir geÃ§iÅŸte Ã§izer.
	 */
	public void render(ICamera camera) {
		if (particles.isEmpty()) return;

		// OpenGL durumunu (lighting, blending, textures vb.) korumak iÃ§in push et
		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

		// 3D Matrix'leri fixed-function pipeline'a yÃ¼kle
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPushMatrix();
		FloatBuffer projBuf = BufferUtils.createFloatBuffer(16);
		camera.getProjectionMatrix().store(projBuf);
		projBuf.flip();
		GL11.glLoadMatrix(projBuf);

		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPushMatrix();
		FloatBuffer viewBuf = BufferUtils.createFloatBuffer(16);
		camera.getViewMatrix().store(viewBuf);
		viewBuf.flip();
		GL11.glLoadMatrix(viewBuf);

		// Shader'Ä± kapat (fixed-function pipeline kullanacaÄŸÄ±z)
		GL20.glUseProgram(0);

		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDepthMask(false); // Derinlik yazÄ±mÄ±nÄ± kapat (transparan Ã§akÄ±ÅŸmalarÄ± engellemek iÃ§in)

		// PartikÃ¼lleri her zaman TEXTURE0 biriminde Ã§iz, diÄŸer doku sÄ±zÄ±ntÄ±larÄ±nÄ± engelle
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);

		for (int i = 1; i <= 4; i++) {
			GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
			GL11.glDisable(GL11.GL_TEXTURE_2D);
		}
		GL13.glActiveTexture(GL13.GL_TEXTURE0);

		// Billboard yÃ¶nelim matrisinden Right ve Up vektÃ¶rlerini Ã§Ä±kar
		FloatBuffer modelview = BufferUtils.createFloatBuffer(16);
		GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);
		float rightX = modelview.get(0);
		float rightY = modelview.get(4);
		float rightZ = modelview.get(8);
		float upX    = modelview.get(1);
		float upY    = modelview.get(5);
		float upZ    = modelview.get(9);

		GL11.glBegin(GL11.GL_QUADS);
		for (Particle p : particles) {
			if (p.type == 3) continue; // Ã‡izgileri (WIND_LINE) bu blokta Ã§izme

			float size = p.scale;
			GL11.glColor4f(p.color.x, p.color.y, p.color.z, p.alpha);

			// Sol Alt KÃ¶ÅŸe
			GL11.glTexCoord2f(0f, 0f);
			GL11.glVertex3f(p.position.x - (rightX + upX) * size, p.position.y - (rightY + upY) * size, p.position.z - (rightZ + upZ) * size);

			// SaÄŸ Alt KÃ¶ÅŸe
			GL11.glTexCoord2f(1f, 0f);
			GL11.glVertex3f(p.position.x + (rightX - upX) * size, p.position.y + (rightY - upY) * size, p.position.z + (rightZ - upZ) * size);

			// SaÄŸ Ãœst KÃ¶ÅŸe
			GL11.glTexCoord2f(1f, 1f);
			GL11.glVertex3f(p.position.x + (rightX + upX) * size, p.position.y + (rightY + upY) * size, p.position.z + (rightZ + upZ) * size);

			// Sol Ãœst KÃ¶ÅŸe
			GL11.glTexCoord2f(0f, 1f);
			GL11.glVertex3f(p.position.x - (rightX - upX) * size, p.position.y - (rightY - upY) * size, p.position.z - (rightZ - upZ) * size);
		}
		GL11.glEnd();

		// --- RÃœZGAR Ã‡Ä°ZGÄ°LERÄ°NÄ° (WIND_LINE - Type 3) Ã‡Ä°Z ---
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0); // KaplamayÄ± kapat
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glLineWidth(3.0f); // Ã‡izgi kalÄ±nlÄ±ÄŸÄ±

		for (Particle p : particles) {
			if (p.type != 3) continue;

			GL11.glBegin(GL11.GL_LINE_STRIP); // SÃ¼rekli kÄ±rÄ±k Ã§izgiler (dalga) Ã§izimi
			int segments = 12; // Bir Ã§izginin kaÃ§ parÃ§adan oluÅŸacaÄŸÄ±
			float tailLength = p.scale * 15.0f; // KuyruÄŸun toplam uzunluÄŸu
			
			for(int i = 0; i <= segments; i++) {
				float t = (float) i / segments; // 0.0'dan 1.0'a kadar oransal ilerleme
				
				// KuyruÄŸun sonuna doÄŸru saydamlaÅŸsÄ±n
				float segmentAlpha = p.alpha * (1.0f - t) * 0.7f;
				GL11.glColor4f(p.color.x, p.color.y, p.color.z, segmentAlpha);
				
				// Dalga efekti iÃ§in sinÃ¼s hesabÄ±
				// life parametresi ve t oranÄ±na gÃ¶re zamanla akan dalgalar
				float waveOffset = (float) Math.sin(p.life * 12.0f - t * 8.0f) * 0.6f;
				
				// HÄ±zÄ±n tersine doÄŸru bir Ã§izgi uzatÄ±yoruz
				float px = p.position.x - p.velocity.x * 0.1f * tailLength * t;
				// Y ekseninde dalgalanma (waveOffset) ekliyoruz
				float py = p.position.y - p.velocity.y * 0.1f * tailLength * t + waveOffset; 
				float pz = p.position.z - p.velocity.z * 0.1f * tailLength * t;
				
				GL11.glVertex3f(px, py, pz);
			}
			GL11.glEnd();
		}

		// Temizlik yap ve doku baÄŸlantÄ±larÄ±nÄ± kes
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		GL11.glDisable(GL11.GL_TEXTURE_2D);

		// DiÄŸer doku birimlerinin durumlarÄ±nÄ± temizle (SÄ±zÄ±ntÄ±yÄ± Ã¶nlemek iÃ§in)
		for (int i = 1; i <= 4; i++) {
			GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
			GL11.glDisable(GL11.GL_TEXTURE_2D);
		}
		
		// Aktif doku birimini sÄ±fÄ±rla
		GL13.glActiveTexture(GL13.GL_TEXTURE0);

		// Matrix stack'leri eski haline yÃ¼kle
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPopMatrix();
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPopMatrix();

		// OpenGL durumunu geri yÃ¼kle
		GL11.glPopAttrib();
	}

	/**
	 * Suya dÃ¼ÅŸen bir nesnenin oluÅŸturacaÄŸÄ± damla sÄ±Ã§ramalarÄ±nÄ± ve yÃ¼zey kÃ¶pÃ¼klerini yaratÄ±r.
	 */
	public void spawnSplash(float x, float y, float z, float impactSpeed) {
		if (Float.isNaN(impactSpeed) || Float.isInfinite(impactSpeed)) {
			impactSpeed = 5.0f;
		}
		if (impactSpeed < 0.0f) {
			impactSpeed = 0.0f;
		} else if (impactSpeed > 50.0f) {
			impactSpeed = 50.0f;
		}

		int sprayCount = (int) (18 + impactSpeed * 2.8f);
		if (sprayCount > 75) sprayCount = 75;

		Vector3f splashColor = new Vector3f(0.85f, 0.93f, 0.98f); // Berrak okyanus beyaz/mavisi

		// 1. Havaya fÄ±rlayan su damlalarÄ± (SPRAY)
		for (int i = 0; i < sprayCount; i++) {
			float angle = (float) (Math.random() * Math.PI * 2);
			float spread = (float) (0.8 + Math.random() * 2.5);
			float velX = (float) (Math.cos(angle) * spread);
			float velZ = (float) (Math.sin(angle) * spread);
			
			// Dikey hÄ±zÄ± dÃ¼ÅŸÃ¼ÅŸ ÅŸiddetine gÃ¶re ayarla
			float velY = (float) (4.0f + Math.random() * (impactSpeed * 0.45f + 4.0f));

			Vector3f pos = new Vector3f(
				x + (float) (Math.random() - 0.5f) * 0.4f,
				y + 0.1f,
				z + (float) (Math.random() - 0.5f) * 0.4f
			);
			Vector3f vel = new Vector3f(velX, velY, velZ);

			float gravity = -17.5f; // DamlalarÄ± aÅŸaÄŸÄ± Ã§eken yerÃ§ekimi
			float life = (float) (0.6 + Math.random() * 0.5);
			float scale = (float) (0.12 + Math.random() * 0.18);

			particles.add(new Particle(pos, vel, gravity, life, scale, splashColor, 0));
		}

		// 2. Su yÃ¼zeyinde geniÅŸleyen dairesel kÃ¶pÃ¼kler (FOAM)
		int foamCount = (int) (6 + impactSpeed * 1.2f);
		if (foamCount > 18) foamCount = 18;
		for (int i = 0; i < foamCount; i++) {
			float angle = (float) (Math.random() * Math.PI * 2);
			float speed = (float) (0.2 + Math.random() * 0.6);
			
			Vector3f pos = new Vector3f(
				x + (float) (Math.random() - 0.5) * 1.2f,
				y,
				z + (float) (Math.random() - 0.5) * 1.2f
			);
			Vector3f vel = new Vector3f((float) Math.cos(angle) * speed, 0, (float) Math.sin(angle) * speed);

			float life = (float) (1.2 + Math.random() * 1.4);
			float scale = (float) (0.35 + Math.random() * 0.5);

			particles.add(new Particle(pos, vel, 0f, life, scale, splashColor, 1));
		}

		// 3. Su altÄ±ndaki hava kabarcÄ±klarÄ± (BUBBLE)
		if (impactSpeed > 4.5f) {
			int bubbleCount = (int) (impactSpeed * 1.5f);
			if (bubbleCount > 25) bubbleCount = 25;
			for (int i = 0; i < bubbleCount; i++) {
				Vector3f pos = new Vector3f(
					x + (float) (Math.random() - 0.5) * 1.0f,
					y - (float) (Math.random() * 2.0f),
					z + (float) (Math.random() - 0.5) * 1.0f
				);
				Vector3f vel = new Vector3f(
					(float) (Math.random() - 0.5) * 0.2f,
					0,
					(float) (Math.random() - 0.5) * 0.2f
				);
				float life = (float) (0.6 + Math.random() * 1.2);
				float scale = (float) (0.06 + Math.random() * 0.08);

				particles.add(new Particle(pos, vel, 0f, life, scale, splashColor, 2));
			}
		}
	}

	/**
	 * SÃ¼rÃ¼klenen cisimlerin arkasÄ±nda bÄ±raktÄ±ÄŸÄ± dalga kÃ¶pÃ¼ÄŸÃ¼nÃ¼ Ã¼retir.
	 */
	public void spawnFoam(float x, float y, float z, float baseScale) {
		Vector3f splashColor = new Vector3f(0.85f, 0.93f, 0.98f);
		Vector3f pos = new Vector3f(
			x + (float) (Math.random() - 0.5) * 0.6f,
			y,
			z + (float) (Math.random() - 0.5) * 0.6f
		);
		Vector3f vel = new Vector3f(
			(float) (Math.random() - 0.5) * 0.15f,
			0,
			(float) (Math.random() - 0.5) * 0.15f
		);
		float life = (float) (0.7 + Math.random() * 0.7);
		float scale = (float) (baseScale * (0.55 + Math.random() * 0.45));

		particles.add(new Particle(pos, vel, 0f, life, scale, splashColor, 1));
	}

	/**
	 * RÃ¼zgarÄ±n yÃ¶nÃ¼nÃ¼ gÃ¶stermek iÃ§in rÃ¼zgar dalgalarÄ±nÄ± temsil eden uzun Ã§izgiler Ã¼retir.
	 */
	public void spawnWindParticle(Vector3f cameraPos, Vector3f windVelocity) {
		float windSpeed = windVelocity.length();
		if (windSpeed < 0.1f) return; // RÃ¼zgar yoksa Ã§izgi spawnlama
		
		// KameranÄ±n etrafÄ±nda daha geniÅŸ bir alana rastgele daÄŸÄ±t
		Vector3f pos = new Vector3f(
			cameraPos.x + (float) ((Math.random() - 0.5) * 100.0f),
			cameraPos.y + (float) (Math.random() * 40.0f) - 5.0f, // Biraz aÅŸaÄŸÄ±dan baÅŸlayabilir
			cameraPos.z + (float) ((Math.random() - 0.5) * 100.0f)
		);
		
		// RÃ¼zgar hÄ±zÄ±yla savrulma, rÃ¼zgar hÄ±zÄ±nÄ± belirginleÅŸtirmek iÃ§in Ã§arpan kullanÄ±ldÄ±
		Vector3f vel = new Vector3f(
			windVelocity.x * 2.0f + (float) (Math.random() - 0.5) * 3.0f,
			windVelocity.y + (float) (Math.random() - 0.5) * 2.0f,
			windVelocity.z * 2.0f + (float) (Math.random() - 0.5) * 3.0f
		);
		
		float life = (float) (1.5 + Math.random() * 2.0);
		// Ã‡izgi uzunluÄŸu Ã§arpanÄ± olarak kullanacaÄŸÄ±z
		float scale = (float) (0.8 + Math.random() * 1.5); 
		Vector3f windColor = new Vector3f(0.9f, 0.95f, 1.0f); // UÃ§uk mavi-beyaz saydam rÃ¼zgar rengi

		// Type 3: RÃ¼zgar Ã‡izgileri (WIND_LINE), yerÃ§ekimi 0
		particles.add(new Particle(pos, vel, 0.0f, life, scale, windColor, 3));
	}

	public void clear() {
		particles.clear();
	}

	/**
	 * Piksel dÃ¼zeyinde yumuÅŸak kenarlÄ±, dairesel partikÃ¼l dokusu (RGBA) oluÅŸturur.
	 */
	private int createProceduralParticleTexture() {
		int textureID = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);

		int size = 32;
		ByteBuffer buffer = BufferUtils.createByteBuffer(size * size * 4);

		for (int y = 0; y < size; y++) {
			float dy = (y - size / 2.0f) / (size / 2.0f);
			for (int x = 0; x < size; x++) {
				float dx = (x - size / 2.0f) / (size / 2.0f);
				float dist = (float) Math.sqrt(dx * dx + dy * dy);

				// YumuÅŸak dairesel sÃ¶nÃ¼mlenme
				float alpha = 1.0f - dist;
				if (alpha < 0f) alpha = 0f;
				alpha = (float) Math.pow(alpha, 2.3f); // Ãœstel sÃ¶nÃ¼m

				byte r = (byte) 255;
				byte g = (byte) 255;
				byte b = (byte) 255;
				byte a = (byte) (alpha * 255);

				buffer.put(r);
				buffer.put(g);
				buffer.put(b);
				buffer.put(a);
			}
		}
		buffer.flip();

		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, size, size, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0); // BaÄŸlantÄ±yÄ± kes

		return textureID;
	}
}

