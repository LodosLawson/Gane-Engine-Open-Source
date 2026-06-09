package shaders;

import org.lwjgl.opengl.GL20;

/**
 * Shader kodundaki bir Doku (Texture / Sampler2D) uniform değişkeni temsil eder.
 * Genellikle bir dokunun hangi "Texture Unit" e (Örn: GL_TEXTURE0, GL_TEXTURE1) 
 * bağlı olduğunu shader'a bildirmek için kullanılır.
 */
public class UniformSampler extends Uniform {

	// Shader'a gönderilmiş olan son doku birimi (Texture Unit) id'si
	private int currentValue;
	// Daha önce bir birim atanıp atanmadığını takip eder
	private boolean used = false;

	/**
	 * Yeni bir Sampler uniform temsilcisi oluşturur.
	 * 
	 * @param name Shader içindeki sampler (örn: "diffuseMap") adı
	 */
	public UniformSampler(String name) {
		super(name);
	}

	/**
	 * Shader'a ilgili doku okuyucusunun hangi OpenGL Doku Birimine (Texture Unit)
	 * bakması gerektiğini söyler. Gereksiz tekrarlı atamaları (caching) engeller.
	 * 
	 * @param texUnit Kullanılacak olan texture unit (0, 1, 2 vb.)
	 */
	public void loadTexUnit(int texUnit) {
		if (!used || currentValue != texUnit) {
			GL20.glUniform1i(super.getLocation(), texUnit);
			used = true;
			currentValue = texUnit;
		}
	}

}
