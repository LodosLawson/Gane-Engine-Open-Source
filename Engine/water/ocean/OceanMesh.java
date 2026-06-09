package water.ocean;

import openglObjects.Vao;

public class OceanMesh {

	private Vao vao;
	private int meshSize;

	public OceanMesh(int meshSize) {
		this.meshSize = meshSize;
		this.vao = generateGrid(meshSize);
	}

	private Vao generateGrid(int size) {
		int count = (size + 1) * (size + 1);
		float[] vertices = new float[count * 3];
		int[] indices = new int[size * size * 6];

		int vertexPointer = 0;
		for (int z = 0; z <= size; z++) {
			for (int x = 0; x <= size; x++) {
				vertices[vertexPointer * 3] = (float)x / size - 0.5f;
				vertices[vertexPointer * 3 + 1] = (float)z / size - 0.5f;
				vertices[vertexPointer * 3 + 2] = 0.0f;
				vertexPointer++;
			}
		}

		int pointer = 0;
		for (int z = 0; z < size; z++) {
			for (int x = 0; x < size; x++) {
				int topLeft = (z * (size + 1)) + x;
				int topRight = topLeft + 1;
				int bottomLeft = ((z + 1) * (size + 1)) + x;
				int bottomRight = bottomLeft + 1;

				indices[pointer++] = topLeft;
				indices[pointer++] = bottomLeft;
				indices[pointer++] = topRight;

				indices[pointer++] = topRight;
				indices[pointer++] = bottomLeft;
				indices[pointer++] = bottomRight;
			}
		}

		Vao vao = Vao.create();
		vao.storeData(indices, count, vertices);
		return vao;
	}

	public Vao getVao() {
		return vao;
	}

	public void cleanUp() {
		vao.delete();
	}
}
