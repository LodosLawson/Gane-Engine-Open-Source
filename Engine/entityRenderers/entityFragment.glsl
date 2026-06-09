#version 150

// Vertex shader'dan gelen texture (kaplama) koordinatları.
in vec2 pass_textureCoords;
// Vertex shader'dan gelen parlaklık değeri (ışık hesaplamasından sonra).
in float pass_brightness;
// Vertex shader'dan gelen nesne xz düzlemi konumu.
in vec2 pass_pos;

// Ekrana basılacak son piksel rengi.
out vec4 out_colour;

// Ana kaplama (texture) resmi.
uniform sampler2D diffuseMap;
// Ekstra kaplama resmi (örneğin parlama/glow haritası).
uniform sampler2D extraMap;
// Ekstra haritanın kullanılıp kullanılmayacağını belirten değişken.
uniform float hasExtraMap;

// Özel bir mesafe etkisi için merkez noktası.
const vec2 center = vec2(-2.53, 3.42);

// I want to use the glsl smoothstep function, but for some unknown reason it doesn't work on my laptop, but only when exported as a jar. Works fine in Eclipse!
// Özel bir yumuşatma geçiş fonksiyonu. GLSL'nin kendi smoothstep fonksiyonuna alternatif olarak yazılmıştır.
float smoothlyStep(float edge0, float edge1, float x){
    float t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
    return t * t * (3.0 - 2.0 * t);
}

// Shader'ın ana fonksiyonu. Her bir piksel için çalışır.
void main(void){
	
	// Kaplama resminden alınan temel renk.
	vec4 diffuseColour = texture(diffuseMap, pass_textureCoords);
	
	// Eğer pikselin görünmezlik (alpha) değeri çok düşükse, pikseli çizme. (Örn: Yaprak kenarları)
	if(diffuseColour.a < 0.5){
		discard;
	}
	
	// Temel parlaklık değeri.
	float brightness = pass_brightness;
	
	// Eğer ekstra parlama haritası varsa, bu pikselin parlayıp parlamadığını kontrol et ve parlaklığı ayarla.
	if(hasExtraMap > 0.5){
		float isGlowing = step(0.5, texture(extraMap, pass_textureCoords).g);
		brightness = mix(brightness, 1.0, isGlowing);
	}
	
	// Temel renge parlaklığı uygula.
	out_colour = diffuseColour * brightness;
	
	// Merkez noktadan uzaklaştıkça nesnelerin rengini beyaza (1.0) karıştırma efekti.
	float disFactor = smoothlyStep(15.0, 16.0, distance(center, pass_pos));
	out_colour.rgb = mix(out_colour.rgb, vec3(1.0), disFactor);
	
}