package skybox.classic;

import openglObjects.Vao;

/**
 * GÃ¶kyÃ¼zÃ¼ kutusunun (Skybox) geometrisini oluÅŸturan yardÄ±mcÄ± sÄ±nÄ±f.
 * Temel olarak belirtilen boyutta basit bir 3B kÃ¼p modeli (VAO) Ã¼retir.
 */
public class CubeGenerator {

	// Bir kÃ¼pÃ¼n kÃ¶ÅŸe sayÄ±sÄ± (Vertex count)
	private static final int VERTEX_COUNT = 8;
	// KÃ¼pÃ¼ oluÅŸturacak Ã¼Ã§genlerin kÃ¶ÅŸe baÄŸlantÄ± sÄ±rasÄ± (Index Array)
	private static final int[] INDICES = { 0, 1, 3, 1, 2, 3, 1, 5, 2, 2, 5, 6, 4, 7, 5, 5, 7, 6, 0,
			3, 4, 4, 3, 7, 7, 3, 6, 6, 3, 2, 4, 5, 0, 0, 5, 1 };

	/**
	 * Verilen boyutta bir kÃ¼p oluÅŸturur ve bunu ekran kartÄ± belleÄŸine (VAO) yÃ¼kler.
	 * 
	 * @param size KÃ¼pÃ¼n boyutu (YarÄ±Ã§ap/GeniÅŸlik oranÄ±)
	 * @return OluÅŸturulan kÃ¼pÃ¼n GPU kimliÄŸini barÄ±ndÄ±ran VAO nesnesi
	 */
	public static Vao generateCube(float size) {
		Vao vao = Vao.create();
		vao.storeData(INDICES, VERTEX_COUNT, getVertexPositions(size));
		return vao;
	}

	/**
	 * KÃ¼pÃ¼n 8 kÃ¶ÅŸesinin (x,y,z) koordinatlarÄ±nÄ± hesaplar.
	 * 
	 * @param size Boyut parametresi
	 * @return KÃ¶ÅŸe koordinatlarÄ±nÄ± iÃ§eren float dizisi
	 */
	private static float[] getVertexPositions(float size) {
		return new float[] { -size, size, size, size, size, size, size, -size, size, -size, -size,
				size, -size, size, -size, size, size, -size, size, -size, -size, -size, -size,
				-size };
	}

}

