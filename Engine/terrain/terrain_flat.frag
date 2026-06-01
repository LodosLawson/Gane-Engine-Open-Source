#version 150

in vec3 pass_worldPos;
in vec3 pass_normal;
in vec2 pass_uv;
in vec4 pass_shadowCoords;

out vec4 out_colour;

uniform sampler2D shadowMap;

uniform sampler2D uGrassTex;
uniform sampler2D uDirtTex;
uniform sampler2D uDirt2Tex;
uniform sampler2D uSandTex;
uniform sampler2D uGrassNormal;
uniform sampler2D uDirtNormal;
uniform sampler2D uDirt2Normal;
uniform sampler2D uSandNormal;
uniform vec3   uLightDir;
uniform vec3   uLightColor;
uniform float uAmbient;
uniform float uTextureScale;

// Nokta Isik (Point Light) Uniformlari
uniform vec3 pointLightPos[4];
uniform vec3 pointLightColor[4];
uniform vec3 pointLightAttenuation[4];

// Fog uniforms
uniform vec3   uFogColor;
uniform float uFogDensity;
uniform float uFogStart;
uniform vec3   uCameraPos;

uniform float uIsPlanetary;
uniform vec3   uPlanetCenter;

// --- CLOUD SHADOW UNIFORMS ---
uniform float uTime;
uniform vec2  uWindDir;
uniform float uCloudShadowEnabled;
uniform vec4  uCloudShadowPos[32];
uniform float uCloudShadowAlpha[32];
uniform int   uNumCloudShadows;

// --- SIMPLE PROCEDURAL NOISE ---
float hash(vec2 p) {
    int ix = int(p.x);
    int iz = int(p.y);
    
    ix = ix % 100000;
    iz = iz % 100000;
    
    if (ix < 0) ix += 100000;
    if (iz < 0) iz += 100000;
    
    int n = ix + iz * 57;
    n = (n << 13) ^ n;
    int nn = (n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff;
    return 1.0 - (float(nn) / 1073741824.0);
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

// Optimized basic sampling (FPS drop fixed)
vec4 sampleDetiled(sampler2D tex, vec2 uv, vec2 worldPos) {
    vec4 t = texture(tex, uv);
    return vec4(pow(clamp(t.rgb, 0.0, 1.0), vec3(2.2)), t.a);
}

vec3 sampleDetiledNormal(sampler2D tex, vec2 uv, vec2 worldPos) {
    return normalize(texture(tex, uv).rgb * 2.0 - 1.0);
}

vec3 calculateTBNNormal(vec3 tangentNormal, vec3 N) {
    vec3 up = abs(N.y) < 0.999 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
    vec3 T = normalize(cross(up, N));
    vec3 B = cross(N, T);
    mat3 TBN = mat3(T, B, N);
    return normalize(TBN * tangentNormal);
}

void main(void) {
    vec3 N = normalize(pass_normal);
    vec2 tiledUV;
    float upness;
    float variationBlend;
    float heightBlend; // Used as beach/shore blend
    float mountainBlend; // Used as rock/cliff height blend
    vec2 worldRef;

    float peakBlend = 0.0;
    if (uIsPlanetary > 0.5) {
        tiledUV = pass_uv * (uTextureScale / 50.0);
        vec3 toSurface = normalize(pass_worldPos - uPlanetCenter);
        upness = clamp(dot(N, toSurface), 0.0, 1.0);
        
        float macroNoise = noise(pass_worldPos.xy / 800.0);
        variationBlend = smoothstep(0.35, 0.65, macroNoise);
        heightBlend = 1.0; 
        mountainBlend = 0.0;
        worldRef = pass_worldPos.xy;
    } else {
        vec2 worldXZ = pass_worldPos.xz;
        tiledUV = worldXZ * (uTextureScale / 5012.0);
        upness = clamp(dot(N, vec3(0.0, 1.0, 0.0)), 0.0, 1.0);
        
        // Remove texture-based macro noise to prevent straight seams, use procedural noise
        mat2 rot = mat2(0.8, -0.6, 0.6, 0.8);
        float macroNoise = noise(rot * worldXZ * 0.002);
        variationBlend = smoothstep(0.35, 0.65, macroNoise);
        
        // Rotate noise to break the grid artifact
        float shoreNoise = (noise(rot * worldXZ * 0.05) * 8.0 - 4.0) + (noise(worldXZ * 0.2) * 2.0 - 1.0);
        heightBlend = smoothstep(2.0, 16.0, pass_worldPos.y + shoreNoise); // shore transition
        
        float mountainNoise = (noise(rot * worldXZ * 0.015) * 16.0 - 8.0) + (noise(worldXZ * 0.08) * 4.0 - 2.0);
        mountainBlend = smoothstep(40.0, 95.0, pass_worldPos.y + mountainNoise); // transition to mountain rock
        worldRef = worldXZ;
    }

    // 3. Slope blend: steep slopes -> rock, flat areas -> others
    float slopeBlend = smoothstep(0.60, 0.85, upness); // 0=steep rock, 1=flat

    // HEIGHT BLENDS (Önce hesapla ki if bloklarında kullanabilelim)
    float sandToPlainsBlend = smoothstep(-1.0, 4.0, pass_worldPos.y + (variationBlend * 2.0 - 1.0));
    float plainsToDirtBlend = smoothstep(15.0, 50.0, pass_worldPos.y + (variationBlend * 10.0 - 5.0));
    float dirtToRockBlend = smoothstep(65.0, 100.0, pass_worldPos.y + (variationBlend * 15.0 - 7.5)); 

    // 2. DİNAMİK TEXTURE FETCH (SADECE GEREKENİ OKU) -> FPS Düşüşünü Çözer
    vec4 sandColor = vec4(0.0); vec3 sandNormal = vec3(0.0);
    if (sandToPlainsBlend < 1.0) {
        sandColor = sampleDetiled(uSandTex, tiledUV, worldRef);
        sandNormal = sampleDetiledNormal(uSandNormal, tiledUV, worldRef);
    }

    vec4 grassColor = vec4(0.0); vec3 grassNormal = vec3(0.0);
    if (sandToPlainsBlend > 0.0 && plainsToDirtBlend < 1.0 && slopeBlend > 0.0) {
        grassColor = sampleDetiled(uGrassTex, tiledUV, worldRef);
        grassNormal = sampleDetiledNormal(uGrassNormal, tiledUV, worldRef);
    }

    vec4 dirtColor = vec4(0.0); vec3 dirtNormal = vec3(0.0);
    if (plainsToDirtBlend > 0.0 && dirtToRockBlend < 1.0 && slopeBlend > 0.0) {
        dirtColor = sampleDetiled(uDirtTex,  tiledUV, worldRef);
        dirtNormal = sampleDetiledNormal(uDirtNormal, tiledUV, worldRef);
    }

    vec4 rockColor = vec4(0.0); vec3 rockNormal = vec3(0.0);
    if (dirtToRockBlend > 0.0 || slopeBlend < 1.0) {
        rockColor = sampleDetiled(uDirt2Tex, tiledUV, worldRef);
        rockNormal = sampleDetiledNormal(uDirt2Normal, tiledUV, worldRef);
    }

    // Yüksekliğe göre temel geçişler
    vec4 flatColor = mix(sandColor, grassColor, sandToPlainsBlend);
    flatColor = mix(flatColor, dirtColor, plainsToDirtBlend);
    flatColor = mix(flatColor, rockColor, dirtToRockBlend);

    // Eğimli alanlarda (steep) kumdan direkt kayaya veya topraktan kayaya geçiş
    vec4 steepLower = mix(sandColor, rockColor, sandToPlainsBlend);
    vec4 steepColor = mix(steepLower, rockColor, plainsToDirtBlend);
    
    // Final blend: Eğime (slope) göre düz alan ile sarp yamaç rengini harmanla
    vec4 finalAlbedo = mix(steepColor, flatColor, slopeBlend);

    // Apply color correction & saturation boost for richer, more realistic tones
    float gray = dot(finalAlbedo.rgb, vec3(0.299, 0.587, 0.114));
    finalAlbedo.rgb = mix(vec3(gray), finalAlbedo.rgb, 1.05); // slight saturation boost
    finalAlbedo.rgb = max(vec3(0.0), (finalAlbedo.rgb - 0.5) * 1.05 + 0.5); // slight contrast boost

    // --- CLOUD SHADOWS ---
    float cloudShadow = 1.0;

    if (uCloudShadowEnabled > 0.5) {
        vec3 D = normalize(-uLightDir);
        float shadowAccum = 0.0;
        for (int i = 0; i < uNumCloudShadows; i++) {
            vec3 C = uCloudShadowPos[i].xyz;
            float R = uCloudShadowPos[i].w;
            float alpha = uCloudShadowAlpha[i];
            if (R > 0.0 && alpha > 0.01) {
                vec3 toCloud = C - pass_worldPos;
                float t = dot(toCloud, D);
                if (t > 0.0) {
                    float distSq = dot(toCloud, toCloud) - t * t;
                    float rSq = R * R;
                    if (distSq < rSq) {
                        float dist = sqrt(distSq);
                        // Bulut merkezine gore goreli pozisyon (noise kilitleme)
                        vec3 intersect = pass_worldPos + D * t;
                        vec3 relPos = intersect - C;
                        // Daha yuksek frekansta FBM noise -> daha keskin bulut sekli
                        vec2 noiseUV = relPos.xz * (1.0 / R) * 3.0 + vec2(uTime * 0.015);
                        float n = noise(noiseUV); // OPTIMIZED: Changed from fbm to noise
                        
                        // Noise ile kenar perturbe: daha genis sapma = daha drametik bulut sekli
                        float perturbedDist = dist + (n - 0.5) * 0.35 * R;
                        // Kuvvetlendirilmis falloff: kareli = daha sert merkez, daha yumusak kenar
                        float edgeFactor = clamp(1.0 - (perturbedDist / R), 0.0, 1.0);
                        float edgeSoftness = edgeFactor * edgeFactor * edgeFactor; // kubik falloff
                        float shadowDensity = edgeSoftness * alpha;
                        
                        // Additive accumulation (ustuste gelen bulutlar daha karanlik)
                        shadowAccum = clamp(shadowAccum + shadowDensity * 0.7, 0.0, 1.0);
                        if (shadowAccum >= 1.0) break; // OPTIMIZED: early exit
                    }
                }
            }
        }
        // 0.15 = maksimum karanlik (%85 karanlik) - belirgin ama gercekci
        cloudShadow = mix(1.0, 0.15, clamp(shadowAccum, 0.0, 1.0));
    }

    // --- LIGHTING (Blinn-Phong + Hemispherical Ambient) ---
    vec3 L = normalize(-uLightDir);
    N = normalize(pass_normal);

    // Normal mapping blending
    vec3 flatNormal = mix(sandNormal, grassNormal, sandToPlainsBlend);
    flatNormal = mix(flatNormal, dirtNormal, plainsToDirtBlend);
    flatNormal = mix(flatNormal, rockNormal, dirtToRockBlend);

    vec3 steepLowerNormal = mix(sandNormal, rockNormal, sandToPlainsBlend);
    vec3 steepNormal = mix(steepLowerNormal, rockNormal, plainsToDirtBlend);
    
    vec3 tangentNormal = mix(steepNormal, flatNormal, slopeBlend);

    N = calculateTBNNormal(tangentNormal, N);

    // --- GLOBAL SHADOW MAPPING ---
    float globalShadow = 1.0;
    
    vec3 projCoords = pass_shadowCoords.xyz / pass_shadowCoords.w;
    
    if (projCoords.x >= 0.0 && projCoords.x <= 1.0 && 
        projCoords.y >= 0.0 && projCoords.y <= 1.0 &&
        projCoords.z >= 0.0 && projCoords.z <= 1.0) {
        
        float shadowMapSize = 2048.0;
        float texelSize = 1.0 / shadowMapSize;
        float currentDepth = projCoords.z;
        
        // Egim olcekli bias - shadow acne'yi onler
        float bias = max(0.005 * (1.0 - dot(N, L)), 0.001);
        
        float totalLight = 0.0;
        
        // 3x3 PCF filtre
        for (float x = -1.0; x <= 1.0; x += 1.0) {
            for (float y = -1.0; y <= 1.0; y += 1.0) {
                float pcfDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
                totalLight += (currentDepth - bias) > pcfDepth ? 0.0 : 1.0;
            }
        }
        globalShadow = totalLight / 9.0;
    } else {
        globalShadow = 1.0;
    }
    
    // Combine cloud shadows and global shadows
    float finalShadow = min(cloudShadow, globalShadow);

    float diffuse = max(dot(N, L), 0.0) * finalShadow;

    vec3 V = normalize(uCameraPos - pass_worldPos);
    vec3 H = normalize(L + V);
    float spec = pow(max(dot(N, H), 0.0), 32.0) * 0.08 * slopeBlend * finalShadow;

    // Realistic Hemispherical Ambient Lighting (Sky light vs Ground reflection)
    vec3 skyAmbient = vec3(0.5, 0.65, 0.8) * uAmbient; // Cool blue sky ambient
    vec3 groundAmbient = vec3(0.2, 0.18, 0.15) * uAmbient; // Warm earthy ground bounce
    float hemiMix = N.y * 0.5 + 0.5;
    vec3 ambient = mix(groundAmbient, skyAmbient, hemiMix);

    vec3 pointLightIllumination = vec3(0.0);
    vec3 pointLightSpecular = vec3(0.0);
    
    for(int i = 0; i < 4; i++) {
        if(pointLightColor[i].r == 0.0 && pointLightColor[i].g == 0.0 && pointLightColor[i].b == 0.0) continue;
        
        vec3 toLightVector = pointLightPos[i] - pass_worldPos;
        float distanceToLight = length(toLightVector);
        vec3 unitLightVector = normalize(toLightVector);
        
        float nDotl = max(dot(N, unitLightVector), 0.0);
        float attFactor = pointLightAttenuation[i].x + (pointLightAttenuation[i].y * distanceToLight) + (pointLightAttenuation[i].z * distanceToLight * distanceToLight);
        
        pointLightIllumination += (pointLightColor[i] * nDotl) / attFactor;
        
        vec3 pointHalfwayVector = normalize(unitLightVector + V);
        float pointSpecularFactor = max(dot(N, pointHalfwayVector), 0.0);
        float pointSpec = pow(pointSpecularFactor, 32.0);
        pointLightSpecular += (pointSpec * 0.08 * slopeBlend * pointLightColor[i]) / attFactor;
    }

    vec3 lighting = uLightColor * diffuse + ambient + vec3(spec) + pointLightIllumination + pointLightSpecular;
    lighting = clamp(lighting, 0.0, 1.5);
    vec3 litColor = finalAlbedo.rgb * lighting;

    // --- GAMMA CORRECTION ---
    litColor = pow(litColor, vec3(1.0 / 2.2));

    // --- DISTANCE FOG ---
    float dist = length(uCameraPos - pass_worldPos);
    float fogFactor = smoothstep(uFogStart, uFogStart * 2.8, dist) * uFogDensity;
    vec3 finalColor = mix(litColor, uFogColor, fogFactor);

    out_colour = vec4(finalColor, finalAlbedo.a);
}