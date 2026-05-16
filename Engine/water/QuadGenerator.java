package water;

import openglObjects.Vao;

/**
 * Su yüzeyini oluşturmak için kullanılan basit bir dörtgen (Quad) üreteci.
 * 2D yatay bir düzlem oluşturarak suyun temel geometrisini sağlar.
 */
public class QuadGenerator {
	
	private static final int VERTEX_COUNT = 4;
	// X ve Z koordinatları (Y ekseni shader içinde konumlandırılır)
	private static final float[] VERTICES = {0, 0, 1, 0, 1, 1, 0, 1};
	// Dörtgeni oluşturacak iki üçgenin indeksleri
	private static final int[] INDICES = {0,3,1,1,3,2};

	/**
	 * Su yüzeyi için gerekli olan VAO'yu (Vertex Array Object) oluşturur.
	 * @return Oluşturulan Vao nesnesi
	 */
	public static Vao generateQuad() {
		Vao vao = Vao.create();
		vao.storeData(INDICES, VERTEX_COUNT, VERTICES);
		return vao;
	}

}
