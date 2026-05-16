package shaders;

import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector3f;

/**
 * Shader kodundaki bir 3 boyutlu vektör (vec3) uniform değişkeni temsil eder.
 * Sıklıkla renk (RGB) veya konum (XYZ) bilgisi yollarken kullanılır.
 * Gereksiz yüklemeleri önlemek için caching içerir.
 */
public class UniformVec3 extends Uniform {
	
	private float currentX;
	private float currentY;
	private float currentZ;
	private boolean used = false;

	/**
	 * Yeni bir Vector3f uniform temsilcisi oluşturur.
	 * 
	 * @param name Shader içindeki vec3 uniform adı
	 */
	public UniformVec3(String name) {
		super(name);
	}

	/**
	 * Vektör nesnesindeki verileri shader'a yükler.
	 * 
	 * @param vector Yüklenecek olan 3B vektör nesnesi
	 */
	public void loadVec3(Vector3f vector) {
		loadVec3(vector.x, vector.y, vector.z);
	}

	/**
	 * Belirtilen X, Y ve Z koordinatlarını (değişmişlerse) shader'a yükler.
	 * 
	 * @param x Vektörün X değeri
	 * @param y Vektörün Y değeri
	 * @param z Vektörün Z değeri
	 */
	public void loadVec3(float x, float y, float z) {
		if (!used || x != currentX || y != currentY || z != currentZ) {
			this.currentX = x;
			this.currentY = y;
			this.currentZ = z;
			used = true;
			GL20.glUniform3f(super.getLocation(), x, y, z);
		}
	}

}
