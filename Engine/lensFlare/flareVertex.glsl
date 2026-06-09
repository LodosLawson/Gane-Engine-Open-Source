#version 330

// Vertexlerin 2 boyutlu girdi pozisyonları
in vec2 in_position;

// Fragment shader'a aktarılacak olan doku koordinatları (texture coords)
out vec2 pass_textureCoords;

// x, y ekran pozisyonu; z, w ise genişlik ve yükseklik (ölçek) bilgisini taşır
uniform vec4 transform;

void main(void){
	
	// -0.5 ve +0.5 arasındaki vertex pozisyonlarını 0.0 - 1.0 UV aralığına çeviriyoruz
	pass_textureCoords = in_position + vec2(0.5, 0.5);
	// OpenGL'de resmin yönünü doğrultmak için Y eksenini ters çevir
	pass_textureCoords.y = 1.0 - pass_textureCoords.y;
	
	// Ekran pozisyonunu ayarla: Merkez koordinatı + (Vertex konumu * Ölçek)
	vec2 screenPosition = in_position * transform.zw + transform.xy;
	//no need for conversion here
	// z derinliğini 0.9999 veriyoruz ki ekrandaki hemen hemen her şeyin önünde olsun
	gl_Position = vec4(screenPosition, 0.9999, 1.0);

}