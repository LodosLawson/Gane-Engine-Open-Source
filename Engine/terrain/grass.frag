#version 150

in vec3 pass_worldPos;
in vec3 pass_normal;
in vec2 pass_uv;
in vec4 pass_shadowCoords;

out vec4 out_colour;

uniform sampler2D uGrassTex;
uniform sampler2D shadowMap;
uniform vec3  uLightDir;
uniform vec3  uLightColor;
uniform float uAmbient;

// Fog uniforms
uniform vec3  uFogColor;
uniform float uFogDensity;
uniform float uFogStart;
uniform vec3  uCameraPos;

// --- CLOUD SHADOW UNIFORMS ---
uniform float uTime;
uniform vec2  uWindDir;
uniform float uCloudShadowEnabled;
uniform vec4  uCloudShadowPos[32];
uniform float uCloudShadowAlpha[32];
uniform int   uNumCloudShadows;

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

void main(void) {
    vec4 texColor = texture(uGrassTex, pass_uv);
    
    // Alpha test to avoid sorting issues
    if (texColor.a < 0.25) {
        discard;
    }

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
                        // Local coordinate calculation: lock noise to the moving cloud cluster center
                        vec3 intersect = pass_worldPos + D * t;
                        vec3 relPos = intersect - C;
                        vec2 noiseUV = relPos.xz * (1.0 / R) * 3.0 + vec2(uTime * 0.015);
                        float n = fbm(noiseUV);
                        
                        float perturbedDist = dist + (n - 0.5) * 0.35 * R;
                        float edgeFactor = clamp(1.0 - (perturbedDist / R), 0.0, 1.0);
                        float edgeSoftness = edgeFactor * edgeFactor * edgeFactor;
                        float shadowDensity = edgeSoftness * alpha;
                        
                        shadowAccum = clamp(shadowAccum + shadowDensity * 0.7, 0.0, 1.0);
                    }
                }
            }
        }
        cloudShadow = mix(1.0, 0.05, clamp(shadowAccum, 0.0, 1.0));
    }

    // Double-sided diffuse lighting
    vec3 N = normalize(pass_normal);
    vec3 L = normalize(-uLightDir);
    
    // --- GLOBAL SHADOW MAPPING ---
    float globalShadow = 1.0;
    vec3 projCoords = pass_shadowCoords.xyz / pass_shadowCoords.w;
    if (projCoords.x >= 0.0 && projCoords.x <= 1.0 && 
        projCoords.y >= 0.0 && projCoords.y <= 1.0 &&
        projCoords.z >= 0.0 && projCoords.z <= 1.0) {
        
        float shadowMapSize = 2048.0;
        float texelSize = 1.0 / shadowMapSize;
        float currentDepth = projCoords.z;
        float bias = max(0.005 * (1.0 - abs(dot(N, L))), 0.002);
        
        float totalLight = 0.0;
        for (float x = -1.0; x <= 1.0; x += 1.0) {
            for (float y = -1.0; y <= 1.0; y += 1.0) {
                float pcfDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
                totalLight += (currentDepth - bias) > pcfDepth ? 0.0 : 1.0;
            }
        }
        globalShadow = totalLight / 9.0;
    }

    float finalShadow = min(cloudShadow, globalShadow);
    float diffuse = abs(dot(N, L)) * finalShadow;
    
    // Cool sky / warm earth ambient mix
    vec3 skyAmbient = vec3(0.5, 0.65, 0.8) * uAmbient;
    vec3 groundAmbient = vec3(0.2, 0.18, 0.15) * uAmbient;
    float hemiMix = N.y * 0.5 + 0.5;
    vec3 ambient = mix(groundAmbient, skyAmbient, hemiMix);

    vec3 lighting = uLightColor * diffuse * 0.8 + ambient;
    vec3 litColor = texColor.rgb * lighting;

    // Gamma correction
    litColor = pow(litColor, vec3(1.0 / 2.2));

    // Distance fog
    float dist = length(uCameraPos - pass_worldPos);
    float fogFactor = smoothstep(uFogStart, uFogStart * 2.8, dist) * uFogDensity;
    vec3 finalColor = mix(litColor, uFogColor, fogFactor);

    out_colour = vec4(finalColor, texColor.a);
}
