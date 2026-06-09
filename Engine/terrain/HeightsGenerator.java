package terrain;

import java.util.Random;

public class HeightsGenerator {

	private static final float AMPLITUDE = 75f;
	private static final int OCTAVES = 3;
	private static final float ROUGHNESS = 0.3f;

	private Random random = new Random();
	private int seed;
	private int xOffset = 0;
	private int zOffset = 0;

	private float amplitude = AMPLITUDE;
	private int octaves = OCTAVES;
	private float roughness = ROUGHNESS;

	public HeightsGenerator() {
		this.seed = random.nextInt(1000000000);
	}

	// only works with POSITIVE gridX and gridZ values!
	public HeightsGenerator(int gridX, int gridZ, int vertexCount, int seed) {
		this.seed = seed;
		xOffset = gridX * (vertexCount - 1);
		zOffset = gridZ * (vertexCount - 1);
	}

	public void setParameters(float amplitude, int octaves, float roughness) {
		this.amplitude = amplitude;
		this.octaves = octaves;
		this.roughness = roughness;
	}

	public float generateHeight(int x, int z) {
		float realX = x + xOffset;
		float realZ = z + zOffset;

		// 1. Continental Noise (Kıta ve Ada oluşumu)
		float continentalFreq = 1.0f / 200.0f; 
		float continentalNoise = getInterpolatedNoise(realX * continentalFreq, realZ * continentalFreq);
		continentalNoise += 0.5f * getInterpolatedNoise(realX * continentalFreq * 2.0f, realZ * continentalFreq * 2.0f);
		
		// Okyanuslar ve karalar
		float baseHeight = continentalNoise * 60f; 

		// 2. Mountain Noise (Dağlık alanları kıtadan bağımsız rastgele üret)
		float mountainFreq = 1.0f / 60.0f;
		float mountainNoise = getInterpolatedNoise(realX * mountainFreq, realZ * mountainFreq);
		
		float mountainMultiplier = 0;
		// Sadece suyun üstünde ve belirli bölgelerde dağlar olsun
		if (baseHeight > -10.0f && mountainNoise > 0.1f) {
			mountainMultiplier = (mountainNoise - 0.1f) / 0.9f;
			// Dağların aniden yükselip sivri olması için karesini alıyoruz
			mountainMultiplier = mountainMultiplier * mountainMultiplier * mountainMultiplier;
		}

		// 3. Nehir (River / Ridged Noise)
		float riverFreq = 1.0f / 50.0f;
		float riverNoise = getInterpolatedNoise(realX * riverFreq, realZ * riverFreq);
		riverNoise += 0.5f * getInterpolatedNoise(realX * riverFreq * 2.0f, realZ * riverFreq * 2.0f);
		
		float valley = Math.abs(riverNoise); 
		
		// Nehirler daha çok görünsün ama sadece karalarda
		float riverDepth = 0f;
		if (valley < 0.15f && baseHeight > 0.0f) { 
			float riverCarve = 1.0f - (valley / 0.15f);
			riverCarve = riverCarve * riverCarve; // Daha keskin V şekli
			riverDepth = -(riverCarve * 70f); // Nehrin derinliği
		}

		// 4. Detail Noise (Kayalık ve ufak engebeler)
		float detailHeight = 0;
		float d = (float) Math.pow(2, octaves - 1);
		for (int i = 0; i < octaves; i++) {
			float freq = (float) (Math.pow(2, i) / d) * 2.0f; 
			float amp = (float) Math.pow(roughness, i) * amplitude;
			detailHeight += getInterpolatedNoise(realX * freq, realZ * freq) * amp;
		}

		// Düzlük (Plains) için daha fazla engebe 
		float flatDetail = detailHeight * 0.4f;

		// Dağlık (Mountains) detayları
		float mountainDetail = detailHeight * mountainMultiplier * 4.0f; 
		
		// Dağları İNANILMAZ BÜYÜK YAP (+350 birim ekstra yükseklik)
		float finalHeight = baseHeight + flatDetail + (mountainMultiplier * 350f) + mountainDetail;
		
		// Nehri uygularken dağlık alanlarda nehirlerin biraz daha sığ olmasını sağla
		if (riverDepth < 0) {
		    finalHeight += riverDepth * (1.0f - (mountainMultiplier * 0.7f));
		}

		return finalHeight;
	}

	private float getInterpolatedNoise(float x, float z) {
		int intX = (int) x;
		int intZ = (int) z;
		float fracX = x - intX;
		float fracZ = z - intZ;

		float v1 = getSmoothNoise(intX, intZ);
		float v2 = getSmoothNoise(intX + 1, intZ);
		float v3 = getSmoothNoise(intX, intZ + 1);
		float v4 = getSmoothNoise(intX + 1, intZ + 1);
		float i1 = interpolate(v1, v2, fracX);
		float i2 = interpolate(v3, v4, fracX);
		return interpolate(i1, i2, fracZ);
	}

	private float interpolate(float a, float b, float blend) {
		double theta = blend * Math.PI;
		float f = (float) (1f - Math.cos(theta)) * 0.5f;
		return a * (1f - f) + b * f;
	}

	private float getSmoothNoise(int x, int z) {
		float corners = (getNoise(x - 1, z - 1) + getNoise(x + 1, z - 1) + getNoise(x - 1, z + 1)
				+ getNoise(x + 1, z + 1)) / 16f;
		float sides = (getNoise(x - 1, z) + getNoise(x + 1, z) + getNoise(x, z - 1)
				+ getNoise(x, z + 1)) / 8f;
		float center = getNoise(x, z) / 4f;
		return corners + sides + center;
	}

	private float getNoise(int x, int z) {
		random.setSeed(x * 49632 + z * 325176 + seed);
		return random.nextFloat() * 2f - 1f;
	}

}
