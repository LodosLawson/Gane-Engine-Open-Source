#version 330

// Vertex shader'dan gelen 2B doku (texture) koordinatları
in vec2 pass_textureCoords;

// Ekrandaki piksele yazılacak son renk çıktısı
out vec4 out_colour;

// Parlamayı içeren doku resmi
uniform sampler2D flareTexture;
// Parlamanın ne kadar parlak / görünür olacağı bilgisi
uniform float brightness;

void main(void){

	// Dokudaki rengi al ve çıktı rengine ata
    out_colour = texture(flareTexture, pass_textureCoords);
    // Güneş ekran merkezinden uzaklaştıkça ve engellendikçe rengin alfa (şeffaflık) değerini kıs
    out_colour.a *= brightness;


}