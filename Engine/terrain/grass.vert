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
uniform float uTime;
uniform vec2 uWindDir;
uniform mat4 toShadowMapSpace;

void main(void) {
    vec4 worldPos = uModel * vec4(inPosition, 1.0);
    
    // Wind Sway logic
    // swayAmount is only applied to the top vertices of the quad (UV.y close to 1.0)
    float swayPower = inUV.y; 
    
    // Add phase shift based on world position to prevent all grass from swaying in unison
    float phase = worldPos.x * 0.4 + worldPos.z * 0.3;
    float windSpeed = 3.0;
    float windSway = sin(uTime * windSpeed + phase) * 0.15;
    windSway += cos(uTime * windSpeed * 2.1 + phase) * 0.05; // secondary harmonic
    
    // Displace in wind direction
    vec2 windForce = vec2(0.0);
    if (length(uWindDir) > 0.001) {
        windForce = normalize(uWindDir) * windSway * swayPower;
    } else {
        windForce = vec2(1.0, 0.0) * windSway * swayPower;
    }
    worldPos.x += windForce.x;
    worldPos.z += windForce.y;

    pass_worldPos = worldPos.xyz;
    pass_normal   = inNormal;
    pass_uv       = inUV;
    pass_shadowCoords = toShadowMapSpace * worldPos;

    gl_ClipDistance[0] = dot(worldPos, uClipPlane);
    gl_Position = uProjection * uView * worldPos;
}
