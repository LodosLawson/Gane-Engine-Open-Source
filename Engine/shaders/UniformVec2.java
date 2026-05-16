package shaders;

import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector2f;

/**
 * Shader kodundaki bir 2 boyutlu vektör (vec2) uniform değişkeni temsil eder.
 * Önbellekleme (caching) ile sadece x veya y değiştiğinde GPU'ya güncelleme gönderir.
 */
public class UniformVec2 extends Uniform {

	private float currentX;
	private float currentY;
	private boolean used = false;

	/**
	 * Yeni bir Vector2f uniform temsilcisi oluşturur.
	 * 
	 * @param name Shader içindeki vec2 uniform adı
	 */
	public UniformVec2(String name) {
		super(name);
	}

	/**
	 * Vektör nesnesindeki verileri shader'a yükler.
	 * 
	 * @param vector Yüklenecek olan 2B vektör
	 */
	public void loadVec2(Vector2f vector) {
		loadVec2(vector.x, vector.y);
	}

	/**
	 * Belirtilen X ve Y koordinatlarını (değişmişlerse) shader'a yükler.
	 * 
	 * @param x Vektörün X değeri
	 * @param y Vektörün Y değeri
	 */
	public void loadVec2(float x, float y) {
		if (!used || x != currentX || y != currentY) {
			this.currentX = x;
			this.currentY = y;
			used = true;
			GL20.glUniform2f(super.getLocation(), x, y);
		}
	}

}
