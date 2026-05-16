package shaders;

import org.lwjgl.opengl.GL20;

/**
 * Shader kodundaki bir 'float' uniform değişkeni temsil eder.
 * Performans için önceki değeri hafızasında tutarak sadece değer değiştiğinde
 * GPU'ya veri gönderir (Caching).
 */
public class UniformFloat extends Uniform{
	
	// En son shader'a yüklenmiş olan float değeri
	private float currentValue;
	// Daha önce shader'a bir değer gönderilip gönderilmediği bayrağı
	private boolean used = false;
	
	/**
	 * Yeni bir Float uniform temsilcisi oluşturur.
	 * 
	 * @param name Shader içindeki float uniform adı
	 */
	public UniformFloat(String name){
		super(name);
	}
	
	/**
	 * GPU'daki shader'a yeni float değerini yükler.
	 * Eğer yeni değer eskisiyle aynıysa, yükleme işlemi (ve performans kaybı) pas geçilir.
	 * 
	 * @param value Yüklenecek yeni float değer
	 */
	public void loadFloat(float value){
		if(!used || currentValue!=value){
			GL20.glUniform1f(super.getLocation(), value);
			used = true;
			currentValue = value;
		}
	}

}
