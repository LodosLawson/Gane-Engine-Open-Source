
#version 430

#define ONE_OVER_4PI	0.0795774715459476

// NOTE: also defined in vertex shader
#define BLEND_START		8		// m
#define BLEND_END		2000		// m

out vec4 my_FragColor0;

in vec3 vdir;
in vec2 tex;
in vec4 clipSpace;
in vec3 pass_worldPos;

layout (binding = 1) uniform sampler2D perlin;
layout (binding = 2) uniform samplerCube envmap;
layout (binding = 3) uniform sampler2D gradients;
layout (binding = 4) uniform sampler2D reflectionTexture;
layout (binding = 5) uniform sampler2D refractionTexture;
layout (binding = 6) uniform sampler2D depthTexture;

uniform float uNearPlane;
uniform float uFarPlane;

uniform vec4 uvParams;
uniform vec2 perlinOffset;
uniform vec3 oceanColor; // Base water color (from user settings)

uniform vec3 lightDirection;
uniform vec3 lightColor;
uniform vec3 lightAmbient;

// --- CLOUD SHADOW UNIFORMS ---
uniform float uTime;
uniform float uCloudShadowEnabled;
uniform vec4  uCloudShadowPos[32];
uniform float uCloudShadowAlpha[32];
uniform int   uNumCloudShadows;

// --- FOG UNIFORMS ---
uniform vec3 uFogColor;
uniform float uFogDensity;
uniform float uFogStart;
uniform vec3 eyePos;

// --- SHIP CUTOUT UNIFORMS ---
uniform float uShipEnabled;
uniform vec3 uShipPos;
uniform float uShipYaw;
uniform vec2 uShipDim;

// --- SIMPLE PROCEDURAL NOISE ---
float hash(vec2 p) {
    p  = fract(p * vec2(5.3983, 5.4427));
    p += dot(p.yx, p.xy + vec2(21.5351, 14.3137));
    return fract(p.x * p.y * 95.4337);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f*f*(3.0-2.0*f);
    return mix(mix(hash(i + vec2(0.0,0.0)), hash(i + vec2(1.0,0.0)), u.x),
               mix(hash(i + vec2(0.0,1.0)), hash(i + vec2(1.0,1.0)), u.x), u.y);
}

// Fractal noise for clouds
float fbm(vec2 p) {
    float f = 0.0;
    f += 0.5000 * noise(p); p = p * 2.02;
    f += 0.2500 * noise(p); p = p * 2.03;
    f += 0.1250 * noise(p); p = p * 2.01;
    f += 0.0625 * noise(p);
    return f;
}

void main()
{
	// NOTE: also defined in vertex shader
	const vec3 perlinFrequency	= vec3(1.12, 0.59, 0.23);
	const vec3 perlinGradient	= vec3(0.014, 0.016, 0.022);
	vec3 sundir = normalize(-lightDirection); // Pointing towards light source

	// --- SHIP CUTOUT LOGIC ---
	// Geminin icine giren sulari gizle (discard)
	if (uShipEnabled > 0.5) {
		vec2 offset = pass_worldPos.xz - uShipPos.xz;
		
		// Modelin yönüne göre ofseti ters çevir (Gemi modeli 90 derece dönük olabilir vs.)
		// Biz rotation.y'yi (radyan) verdik.
		float c = cos(-uShipYaw);
		float s = sin(-uShipYaw);
		vec2 localOffset = vec2(offset.x * c - offset.y * s, offset.x * s + offset.y * c);
		
		// Elips formülü x^2 / a^2 + y^2 / b^2 < 1.0 (a=uzunluk, b=genişlik)
		float distSq = (localOffset.x * localOffset.x) / (uShipDim.x * uShipDim.x) + 
					   (localOffset.y * localOffset.y) / (uShipDim.y * uShipDim.y);
					   
		if (distSq < 1.0) {
			discard; // Geminin içindeki suyu çizme!
		}
	}

	// blend with Perlin waves
	float dist = length(vdir.xz);
	float factor = (BLEND_END - dist) / (BLEND_END - BLEND_START);
	vec2 perl = vec2(0.0);

	factor = clamp(factor * factor * factor, 0.0, 1.0);

	if (factor < 1.0) {
		vec2 ptex = tex + uvParams.zw;

		vec2 p0 = texture(perlin, ptex * perlinFrequency.x + perlinOffset).rg * 2.0 - vec2(1.0);
		vec2 p1 = texture(perlin, ptex * perlinFrequency.y + perlinOffset).rg * 2.0 - vec2(1.0);
		vec2 p2 = texture(perlin, ptex * perlinFrequency.z + perlinOffset).rg * 2.0 - vec2(1.0);

		perl = (p0 * perlinGradient.x + p1 * perlinGradient.y + p2 * perlinGradient.z);
	}

	// calculate thingies
	vec4 grad = texture(gradients, tex);
	grad.xy = mix(perl, grad.xy, factor);

	vec3 n = normalize(grad.xzy);
	vec3 v = normalize(vdir);
	vec3 l = reflect(-v, n);

	// Calculate distortion based on normal
	vec2 distortion = n.xz * 0.05;

	// Calculate Normalized Device Coordinates (NDC) for planar reflections
	vec2 ndc = (clipSpace.xy / clipSpace.w) / 2.0 + 0.5;
	
	// Refraction coordinates
	vec2 refractTexCoords = vec2(ndc.x, ndc.y) + distortion;
	refractTexCoords = clamp(refractTexCoords, 0.001, 0.999);
	
	// Reflection coordinates (Camera is flipped around Y axis in reflection pass)
	vec2 reflectTexCoords = vec2(ndc.x, 1.0 - ndc.y) + distortion;
	reflectTexCoords.x = clamp(reflectTexCoords.x, 0.001, 0.999);
	reflectTexCoords.y = clamp(reflectTexCoords.y, 0.001, 0.999);

	// Fresnel term (Schlick's approximation)
	float F0 = 0.020018673;
	float ndotv = clamp(dot(n, v), 0.0, 1.0);
	float F = F0 + (1.0 - F0) * pow(1.0 - ndotv, 5.0);

	// Sample Framebuffer Textures
	vec3 refrColor = texture(refractionTexture, refractTexCoords).rgb;
	vec3 reflColor = texture(reflectionTexture, reflectTexCoords).rgb;

	// Fallback if no refraction rendered
	if (length(refrColor) < 0.05) {
		refrColor = oceanColor;
	}

	// Mix refraction with deep ocean color based on depth/water clarity 
	// To match the realistic dark ocean photo
	vec3 deepTint = vec3(0.01, 0.02, 0.03);
	vec3 deepWaterColor = mix(refrColor, deepTint, 0.9); // 90% dark tint
	
	// If reflection texture is black/empty, fallback to a cloudy sky color instead of cubemap
	if (length(reflColor) < 0.05) {
		vec3 skyColor = mix(vec3(0.6, 0.65, 0.7), vec3(0.15, 0.2, 0.25), clamp(l.y, 0.0, 1.0));
		// Scale fallback reflection by ambient light so it doesn't glow white at night!
		reflColor = skyColor * clamp(lightAmbient * 2.5, 0.01, 1.0);
	}

	// Calculate turbulence/foam
	float turbulence = max(1.6 - grad.w, 0.0);
	float foamAmount = smoothstep(1.2, 1.8, turbulence);
	foamAmount = mix(0.0, foamAmount, factor);

	// --- CLOUD SHADOWS ---
	float cloudShadow = 1.0;
	if (uCloudShadowEnabled > 0.5) {
		float shadowAccum = 0.0;
		for (int i = 0; i < uNumCloudShadows; i++) {
			vec3 C = uCloudShadowPos[i].xyz;
			float R = uCloudShadowPos[i].w;
			float alpha = uCloudShadowAlpha[i];
			if (R > 0.0 && alpha > 0.01) {
				vec3 toCloud = C - pass_worldPos;
				float t = dot(toCloud, sundir);
				if (t > 0.0) {
					float distSq = dot(toCloud, toCloud) - t * t;
					float rSq = R * R;
					if (distSq < rSq) {
						float dist = sqrt(distSq);
						// Local coordinate calculation: lock noise to the moving cloud cluster center
						vec3 intersect = pass_worldPos + sundir * t;
						vec3 relPos = intersect - C;
						vec2 noiseUV = relPos.xz * (1.0 / R) * 2.0 + vec2(uTime * 0.02);
						float n = noise(noiseUV); // OPTIMIZED: Changed from fbm to noise
						
						// Perturb the distance with scale-independent noise to make the boundary fluffy
						float perturbedDist = dist + (n - 0.5) * 0.25 * R;
						float edgeSoftness = clamp(1.0 - (perturbedDist / R), 0.0, 1.0);
						float shadowDensity = edgeSoftness * alpha;
						
						shadowAccum = max(shadowAccum, shadowDensity);
						if (shadowAccum >= 1.0) break; // OPTIMIZED: early exit
					}
				}
			}
		}
		cloudShadow = mix(1.0, 0.20, clamp(shadowAccum, 0.0, 1.0));
	}

	// Specular highlight (Ward BRDF for realistic sun path on water)
	float ndoth = max(dot(n, normalize(sundir + v)), 0.001);
	float ndotl = clamp(dot(n, sundir), 0.0, 1.0);
	
	const float rho = 0.2; // Decreased specular intensity
	const float ax = 0.08;
	const float ay = 0.02; // Narrower sun path highlight
	
	vec3 h = normalize(sundir + v);
	vec3 x = cross(sundir, n);
	vec3 y = cross(x, n);
	
	float mult = (ONE_OVER_4PI * rho / (ax * ay * sqrt(max(1e-5, ndotl * ndotv))));
	float hdotx = dot(h, x) / ax;
	float hdoty = dot(h, y) / ay;
	
	float spec = mult * exp(-((hdotx * hdotx) + (hdoty * hdoty)) / (ndoth * ndoth));
	spec = clamp(spec, 0.0, 5.0); // Allow bright specular

	// Mix refraction and reflection
	vec3 finalSurface = mix(deepWaterColor * cloudShadow, reflColor, F);
	
	// Add foam on top of water
	vec3 foamColor = vec3(0.9, 0.95, 1.0) * (lightColor * ndotl + lightAmbient);
	
	// --- SOFT EDGES (DEPTH BLENDING) ---
	float depthMapValue = texture(depthTexture, refractTexCoords).r;
	// Calculate linear depths
	float floorDistance = 2.0 * uNearPlane * uFarPlane / (uFarPlane + uNearPlane - (2.0 * depthMapValue - 1.0) * (uFarPlane - uNearPlane));
	float waterDistance = 2.0 * uNearPlane * uFarPlane / (uFarPlane + uNearPlane - (2.0 * gl_FragCoord.z - 1.0) * (uFarPlane - uNearPlane));
	float waterDepth = floorDistance - waterDistance;
	
	// Soft alpha transition over the last 2.5 meters
	float edgeAlpha = clamp(waterDepth / 2.5, 0.0, 1.0);
	
	// Add some natural foam exactly at the shoreline
	float shorelineFoam = clamp(1.0 - abs(waterDepth - 1.25) / 1.25, 0.0, 1.0) * 0.5;
	foamAmount = clamp(foamAmount + shorelineFoam, 0.0, 1.0);
	
	finalSurface = mix(finalSurface, foamColor, foamAmount);
	
	// Add sun specular and ambient light
	vec3 litColor = finalSurface + (spec * lightColor * ndotl * cloudShadow) + (deepTint * lightAmbient * 0.1);
	
	// Çok yüksek parlamaların ufukta sisi delip geçmesini engellemek için sınırla
	litColor = clamp(litColor, 0.0, 1.5);

	// --- DISTANCE FOG (SKY MERGE) ---
	float distToCamera = length(eyePos - pass_worldPos);
	float fogFactor = clamp(smoothstep(uFogStart, uFogStart * 8.0, distToCamera) * uFogDensity, 0.0, 1.0);
	
	// Okyanus ufukta "gri sis" yerine gökyüzünün yansımasına (reflColor) geçiş yapsın.
	// Böylece gökyüzü mavi olduğunda okyanus da uzakta mükemmel bir şekilde o maviye karışır.
	vec3 finalColorWithFog = mix(litColor, reflColor, fogFactor);

	// Okyanusun ufukta keskin kesilmesini onlemek icin, sis yoğunlaştıkça alpha'yı 1.0'a zorla
	float finalAlpha = mix(edgeAlpha, 1.0, fogFactor);

	my_FragColor0 = vec4(finalColorWithFog, finalAlpha);
}
