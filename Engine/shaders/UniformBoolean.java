package shaders;

import org.lwjgl.opengl.GL20;

/**
 * Shader kodundaki bir 'boolean' (veya 0.0/1.0 float eşdeğeri) uniform değişkeni temsil eder.
 * Gereksiz OpenGL çağrılarını (state changes) önlemek için önbellekleme (caching) yapar.
 */
public class UniformBoolean extends Uniform{

	// Bellekte tutulan son yüklenmiş değer
	private boolean currentBool;
	// Bu değişkene daha önce hiç değer atanıp atanmadığı durumu
	private boolean used = false;
	
	/**
	 * Yeni bir Boolean uniform temsilcisi oluşturur.
	 * 
	 * @param name Shader içindeki boolean uniform adı
	 */
	public UniformBoolean(String name){
		super(name);
	}
	
	/**
	 * Eğer değer değiştiyse veya daha önce hiç yüklenmediyse, shader'a yeni değeri yükler.
	 * OpenGL'de boolean değerler genelde float 1.0 (true) veya 0.0 (false) olarak temsil edilir.
	 * 
	 * @param bool Yüklenecek yeni boolean değer
	 */
	public void loadBoolean(boolean bool){
		if(!used || currentBool != bool){
			GL20.glUniform1f(super.getLocation(), bool ? 1f : 0f);
			used = true;
			currentBool = bool;
		}
	}
	
}
