package shaders;

import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector4f;

/**
 * Shader kodundaki bir 4 boyutlu vektör (vec4) uniform değişkeni temsil eder.
 * Renkler (RGBA) veya clipping plane (Kırpma Düzlemi) denklemleri için sıkça kullanılır.
 * Diğer vektör sınıflarından farklı olarak caching YAPMAZ, her çağrıldığında GPU'ya veri yollar.
 */
public class UniformVec4 extends Uniform {

	/**
	 * Yeni bir Vector4f uniform temsilcisi oluşturur.
	 * 
	 * @param name Shader içindeki vec4 uniform adı
	 */
	public UniformVec4(String name) {
		super(name);
	}

	/**
	 * Vektör nesnesindeki verileri shader'a yükler.
	 * 
	 * @param vector Yüklenecek olan 4B vektör
	 */
	public void loadVec4(Vector4f vector) {
		loadVec4(vector.x, vector.y, vector.z, vector.w);
	}

	/**
	 * Belirtilen 4 bileşeni doğrudan shader'a yükler.
	 * Caching (Önbellekleme) mekanizması kullanılmaz.
	 * 
	 * @param x Vektörün X bileşeni
	 * @param y Vektörün Y bileşeni
	 * @param z Vektörün Z bileşeni
	 * @param w Vektörün W bileşeni
	 */
	public void loadVec4(float x, float y, float z, float w) {
		GL20.glUniform4f(super.getLocation(), x, y, z, w);
	}

}
