package skybox;

import openglObjects.Vao;

/**
 * Gökyüzü kutusunun (Skybox) geometrisini oluşturan yardımcı sınıf.
 * Temel olarak belirtilen boyutta basit bir 3B küp modeli (VAO) üretir.
 */
public class CubeGenerator {

	// Bir küpün köşe sayısı (Vertex count)
	private static final int VERTEX_COUNT = 8;
	// Küpü oluşturacak üçgenlerin köşe bağlantı sırası (Index Array)
	private static final int[] INDICES = { 0, 1, 3, 1, 2, 3, 1, 5, 2, 2, 5, 6, 4, 7, 5, 5, 7, 6, 0,
			3, 4, 4, 3, 7, 7, 3, 6, 6, 3, 2, 4, 5, 0, 0, 5, 1 };

	/**
	 * Verilen boyutta bir küp oluşturur ve bunu ekran kartı belleğine (VAO) yükler.
	 * 
	 * @param size Küpün boyutu (Yarıçap/Genişlik oranı)
	 * @return Oluşturulan küpün GPU kimliğini barındıran VAO nesnesi
	 */
	public static Vao generateCube(float size) {
		Vao vao = Vao.create();
		vao.storeData(INDICES, VERTEX_COUNT, getVertexPositions(size));
		return vao;
	}

	/**
	 * Küpün 8 köşesinin (x,y,z) koordinatlarını hesaplar.
	 * 
	 * @param size Boyut parametresi
	 * @return Köşe koordinatlarını içeren float dizisi
	 */
	private static float[] getVertexPositions(float size) {
		return new float[] { -size, size, size, size, size, size, size, -size, size, -size, -size,
				size, -size, size, -size, size, size, -size, size, -size, -size, -size, -size,
				-size };
	}

}
