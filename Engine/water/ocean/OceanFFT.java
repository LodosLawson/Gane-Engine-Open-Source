package water.ocean;

import java.nio.FloatBuffer;
import java.util.Random;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;

import shaders.ComputeShader;
import utils.MyFile;
import org.lwjgl.util.vector.Vector2f;

public class OceanFFT {

	public static final int DISP_MAP_SIZE = 256;
	public static final float PATCH_SIZE = 20.0f;
	public static final float GRAV_ACCELERATION = 9.81f;
	public static final float WIND_SPEED = 1.5f;
	public static final float AMPLITUDE_CONSTANT = 0.10f * 1e-3f;
	public static final Vector2f WIND_DIRECTION = new Vector2f(-0.4f, -0.9f);

	private int initialTexture;
	private int frequenciesTexture;
	private int[] updatedTextures = new int[2];
	private int tempdataTexture;
	private int displacementTexture;
	private int gradientsTexture;

	private ComputeShader updateSpectrum;
	private ComputeShader fourierFFT;
	private ComputeShader createDisp;
	private ComputeShader createGrad;

	public OceanFFT() {
		initShaders();
		initTextures();
	}

	private void initShaders() {
		updateSpectrum = new ComputeShader(new MyFile("water/ocean/updatespectrum.comp"));
		fourierFFT = new ComputeShader(new MyFile("water/ocean/fourier_fft.comp"));
		createDisp = new ComputeShader(new MyFile("water/ocean/createdisplacement.comp"));
		createGrad = new ComputeShader(new MyFile("water/ocean/creategradients.comp"));
	}

	private float phillips(Vector2f k, Vector2f w, float V, float A) {
		float L = (V * V) / GRAV_ACCELERATION;
		float l = L / 1000.0f;
		float kdotw = Vector2f.dot(k, w);
		float k2 = Vector2f.dot(k, k);

		if (k2 < 1e-6f)
			return 0.0f;

		float P_h = A * (float) Math.exp(-1.0f / (k2 * L * L)) / (k2 * k2 * k2) * (kdotw * kdotw);

		if (kdotw < 0.0f) {
			P_h *= 0.07f;
		}
		return P_h * (float) Math.exp(-k2 * l * l);
	}

	private void initTextures() {
		Random random = new Random();

		int sizeWithBorder = DISP_MAP_SIZE + 1;
		FloatBuffer h0data = BufferUtils.createFloatBuffer(sizeWithBorder * sizeWithBorder * 2);
		FloatBuffer wdata = BufferUtils.createFloatBuffer(sizeWithBorder * sizeWithBorder);

		Vector2f w = new Vector2f(WIND_DIRECTION);
		w.normalise();

		int start = DISP_MAP_SIZE / 2;
		for (int m = 0; m <= DISP_MAP_SIZE; ++m) {
			float ky = (float) (2.0 * Math.PI * (start - m)) / PATCH_SIZE;
			for (int n = 0; n <= DISP_MAP_SIZE; ++n) {
				float kx = (float) (2.0 * Math.PI * (start - n)) / PATCH_SIZE;
				Vector2f k = new Vector2f(kx, ky);

				float sqrt_P_h = 0;
				if (kx != 0.0f || ky != 0.0f) {
					sqrt_P_h = (float) Math.sqrt(phillips(k, w, WIND_SPEED, AMPLITUDE_CONSTANT));
				}

				float gaussA = (float) (random.nextGaussian());
				float gaussB = (float) (random.nextGaussian());

				float h0_a = sqrt_P_h * gaussA * 0.70710678f;
				float h0_b = sqrt_P_h * gaussB * 0.70710678f;

				h0data.put(h0_a);
				h0data.put(h0_b);

				float freq = (float) Math.sqrt(GRAV_ACCELERATION * k.length());
				wdata.put(freq);
			}
		}
		h0data.flip();
		wdata.flip();

		// initial (RG32F)
		initialTexture = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, initialTexture);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RG32F, sizeWithBorder, sizeWithBorder, 0, GL30.GL_RG,
				GL11.GL_FLOAT, h0data);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

		// frequencies (R32F)
		frequenciesTexture = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, frequenciesTexture);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R32F, sizeWithBorder, sizeWithBorder, 0, GL11.GL_RED,
				GL11.GL_FLOAT, wdata);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

		// updated[2] (RG32F)
		for (int i = 0; i < 2; i++) {
			updatedTextures[i] = GL11.glGenTextures();
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, updatedTextures[i]);
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RG32F, DISP_MAP_SIZE, DISP_MAP_SIZE, 0, GL30.GL_RG,
					GL11.GL_FLOAT, (FloatBuffer) null);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		}

		// tempdata (RG32F)
		tempdataTexture = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, tempdataTexture);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RG32F, DISP_MAP_SIZE, DISP_MAP_SIZE, 0, GL30.GL_RG,
				GL11.GL_FLOAT, (FloatBuffer) null);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

		// displacement (RGBA32F)
		displacementTexture = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, displacementTexture);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, DISP_MAP_SIZE, DISP_MAP_SIZE, 0, GL11.GL_RGBA,
				GL11.GL_FLOAT, (FloatBuffer) null);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);

		// gradients (RGBA16F)
		gradientsTexture = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, gradientsTexture);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, DISP_MAP_SIZE, DISP_MAP_SIZE, 0, GL11.GL_RGBA,
				GL11.GL_FLOAT, (FloatBuffer) null);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
	}

	public void update(float time) {
		// Update spectrum
		updateSpectrum.start();
		// We can't use glUniform1f directly if we don't have uniforms set up in
		// ComputeShader wrapper,
		// but wait, the shader needs `time`!
		int timeLoc = GL20.glGetUniformLocation(updateSpectrum.getProgramID(), "time");
		GL20.glUniform1f(timeLoc, time);

		GL42.glBindImageTexture(0, initialTexture, 0, false, 0, GL15.GL_READ_ONLY, GL30.GL_RG32F);
		GL42.glBindImageTexture(1, frequenciesTexture, 0, false, 0, GL15.GL_READ_ONLY, GL30.GL_R32F);
		GL42.glBindImageTexture(2, updatedTextures[0], 0, false, 0, GL15.GL_WRITE_ONLY, GL30.GL_RG32F);
		GL42.glBindImageTexture(3, updatedTextures[1], 0, false, 0, GL15.GL_WRITE_ONLY, GL30.GL_RG32F);

		updateSpectrum.dispatch(DISP_MAP_SIZE / 16, DISP_MAP_SIZE / 16, 1);
		updateSpectrum.stop();

		GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

		// FFT on updated[0] -> horizontal pass
		fourierFFT.start();
		GL42.glBindImageTexture(0, updatedTextures[0], 0, false, 0, GL15.GL_READ_ONLY, GL30.GL_RG32F);
		GL42.glBindImageTexture(1, tempdataTexture, 0, false, 0, GL15.GL_WRITE_ONLY, GL30.GL_RG32F);
		fourierFFT.dispatch(DISP_MAP_SIZE, 1, 1);
		fourierFFT.stop();

		GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

		// vertical pass
		fourierFFT.start();
		GL42.glBindImageTexture(0, tempdataTexture, 0, false, 0, GL15.GL_READ_ONLY, GL30.GL_RG32F);
		GL42.glBindImageTexture(1, updatedTextures[0], 0, false, 0, GL15.GL_WRITE_ONLY, GL30.GL_RG32F);
		fourierFFT.dispatch(DISP_MAP_SIZE, 1, 1);
		fourierFFT.stop();

		GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

		// FFT on updated[1] -> horizontal pass
		fourierFFT.start();
		GL42.glBindImageTexture(0, updatedTextures[1], 0, false, 0, GL15.GL_READ_ONLY, GL30.GL_RG32F);
		GL42.glBindImageTexture(1, tempdataTexture, 0, false, 0, GL15.GL_WRITE_ONLY, GL30.GL_RG32F);
		fourierFFT.dispatch(DISP_MAP_SIZE, 1, 1);
		fourierFFT.stop();

		GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

		// vertical pass
		fourierFFT.start();
		GL42.glBindImageTexture(0, tempdataTexture, 0, false, 0, GL15.GL_READ_ONLY, GL30.GL_RG32F);
		GL42.glBindImageTexture(1, updatedTextures[1], 0, false, 0, GL15.GL_WRITE_ONLY, GL30.GL_RG32F);
		fourierFFT.dispatch(DISP_MAP_SIZE, 1, 1);
		fourierFFT.stop();

		GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

		// Create Displacement
		createDisp.start();
		GL42.glBindImageTexture(0, updatedTextures[0], 0, false, 0, GL15.GL_READ_ONLY, GL30.GL_RG32F);
		GL42.glBindImageTexture(1, updatedTextures[1], 0, false, 0, GL15.GL_READ_ONLY, GL30.GL_RG32F);
		GL42.glBindImageTexture(2, displacementTexture, 0, false, 0, GL15.GL_WRITE_ONLY, GL30.GL_RGBA32F);
		createDisp.dispatch(DISP_MAP_SIZE / 16, DISP_MAP_SIZE / 16, 1);
		createDisp.stop();

		GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

		// Create Gradients
		createGrad.start();
		GL42.glBindImageTexture(0, displacementTexture, 0, false, 0, GL15.GL_READ_ONLY, GL30.GL_RGBA32F);
		GL42.glBindImageTexture(1, gradientsTexture, 0, false, 0, GL15.GL_WRITE_ONLY, GL30.GL_RGBA16F);
		createGrad.dispatch(DISP_MAP_SIZE / 16, DISP_MAP_SIZE / 16, 1);
		createGrad.stop();

		GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
	}

	public int getDisplacementTexture() {
		return displacementTexture;
	}

	public int getGradientsTexture() {
		return gradientsTexture;
	}

	public void cleanUp() {
		updateSpectrum.cleanUp();
		fourierFFT.cleanUp();
		createDisp.cleanUp();
		createGrad.cleanUp();

		GL11.glDeleteTextures(initialTexture);
		GL11.glDeleteTextures(frequenciesTexture);
		GL11.glDeleteTextures(updatedTextures[0]);
		GL11.glDeleteTextures(updatedTextures[1]);
		GL11.glDeleteTextures(tempdataTexture);
		GL11.glDeleteTextures(displacementTexture);
		GL11.glDeleteTextures(gradientsTexture);
	}
}
