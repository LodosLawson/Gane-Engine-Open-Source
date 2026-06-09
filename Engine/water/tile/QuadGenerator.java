package water.tile;

import openglObjects.Vao;

/**
 * Su yÃ¼zeyini oluÅŸturmak iÃ§in kullanÄ±lan basit bir dÃ¶rtgen (Quad) Ã¼reteci.
 * 2D yatay bir dÃ¼zlem oluÅŸturarak suyun temel geometrisini saÄŸlar.
 */
public class QuadGenerator {
	
	private static final int GRID_SIZE = 256;

	/**
	 * Su yÃ¼zeyi iÃ§in gerekli olan VAO'yu (Vertex Array Object) oluÅŸturur.
	 * 256x256 Ã§Ã¶zÃ¼nÃ¼rlÃ¼ÄŸÃ¼nde detaylÄ± bir vertex Ä±zgarasÄ± Ã¼reterek Gerstner dalga hareketlerini gÃ¶rselleÅŸtirir.
	 * @return OluÅŸturulan Vao nesnesi
	 */
	public static Vao generateQuad() {
		int vertexCount = GRID_SIZE * GRID_SIZE;
		float[] vertices = new float[vertexCount * 2];
		int[] indices = new int[(GRID_SIZE - 1) * (GRID_SIZE - 1) * 6];

		int vertexPointer = 0;
		for (int i = 0; i < GRID_SIZE; i++) {
			float z = ((float) i / (float) (GRID_SIZE - 1)) - 0.5f;
			for (int j = 0; j < GRID_SIZE; j++) {
				float x = ((float) j / (float) (GRID_SIZE - 1)) - 0.5f;
				vertices[vertexPointer * 2] = x;
				vertices[vertexPointer * 2 + 1] = z;
				vertexPointer++;
			}
		}

		int indexPointer = 0;
		for (int gz = 0; gz < GRID_SIZE - 1; gz++) {
			for (int gx = 0; gx < GRID_SIZE - 1; gx++) {
				int topLeft = (gz * GRID_SIZE) + gx;
				int topRight = topLeft + 1;
				int bottomLeft = ((gz + 1) * GRID_SIZE) + gx;
				int bottomRight = bottomLeft + 1;

				// Triangle 1 (CCW)
				indices[indexPointer++] = topLeft;
				indices[indexPointer++] = bottomLeft;
				indices[indexPointer++] = topRight;

				// Triangle 2 (CCW)
				indices[indexPointer++] = topRight;
				indices[indexPointer++] = bottomLeft;
				indices[indexPointer++] = bottomRight;
			}
		}

		Vao vao = Vao.create();
		vao.storeData(indices, vertexCount, vertices);
		return vao;
	}

}

