#version 150

in vec3 inPosition;
in vec3 inNormal;
in vec2 inUV;

out vec3 pass_worldPos;
out vec3 pass_normal;
out vec2 pass_uv;
out vec4 pass_shadowCoords;

uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProjection;
uniform vec4 uClipPlane;
uniform mat4 toShadowMapSpace;

// Infinite Terrain Uniforms
uniform float uInfinite;
uniform float uMaxHeight;
uniform float uRoughness;
uniform int uOctaves;
uniform float uScale;
uniform float uOffsetX;
uniform float uOffsetZ;
uniform float uBaseHeight;

// Hash function for random values
float hash(vec2 p) {
    int ix = int(floor(p.x + 1000000.0));
    int iz = int(floor(p.y + 1000000.0));
    
    ix = ix % 100000;
    iz = iz % 100000;
    
    int n = ix + iz * 57;
    n = (n << 13) ^ n;
    int nn = (n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff;
    return 1.0 - (float(nn) / 1073741824.0);
}

// 2D Value Noise
float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f); // smoothstep

    float a = hash(i + vec2(0.0, 0.0));
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

// Fractional Brownian Motion
float fbm(vec2 p) {
    float total = 0.0;
    float amplitude = 1.0;
    float maxValue = 0.0;
    
    for (int i = 0; i < uOctaves; i++) {
        total += noise(p) * amplitude;
        maxValue += amplitude;
        p *= 2.0;
        amplitude *= uRoughness;
    }
    return total / maxValue;
}

void main(void) {
    vec4 worldPos = uModel * vec4(inPosition, 1.0);
    vec3 calculatedNormal = inNormal;
    
    if (uInfinite > 0.5) {
        // Compute procedural height on GPU
        vec2 noisePos = vec2(worldPos.x / uScale + uOffsetX, worldPos.z / uScale + uOffsetZ);
        float h = fbm(noisePos) * uMaxHeight;
        worldPos.y = h + uBaseHeight;
        
        // Approximate normals using finite difference
        float eps = 0.5; // Sampling distance
        float hL = fbm(vec2((worldPos.x - eps) / uScale + uOffsetX, worldPos.z / uScale + uOffsetZ)) * uMaxHeight;
        float hR = fbm(vec2((worldPos.x + eps) / uScale + uOffsetX, worldPos.z / uScale + uOffsetZ)) * uMaxHeight;
        float hD = fbm(vec2(worldPos.x / uScale + uOffsetX, (worldPos.z - eps) / uScale + uOffsetZ)) * uMaxHeight;
        float hU = fbm(vec2(worldPos.x / uScale + uOffsetX, (worldPos.z + eps) / uScale + uOffsetZ)) * uMaxHeight;
        
        vec3 normal = normalize(vec3(hL - hR, 2.0 * eps, hD - hU));
        calculatedNormal = normal;
    }
    
    pass_worldPos = worldPos.xyz;
    pass_normal   = normalize(calculatedNormal);
    pass_uv       = inUV;
    pass_shadowCoords = toShadowMapSpace * worldPos;

    gl_ClipDistance[0] = dot(worldPos, uClipPlane);
    gl_Position = uProjection * uView * worldPos;
}
