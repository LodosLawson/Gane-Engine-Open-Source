package skybox;

import java.util.ArrayList;
import java.util.List;

import openglObjects.Vao;

/**
 * Belirli bir yarıçapta 3 boyutlu küre (Sphere) Vao nesnesi üreten yardımcı sınıf.
 * Atmosfer ve Güneş objeleri gibi büyük gezegensel objeler için kullanılır.
 */
public class SphereGenerator {

	public static Vao generateSphere(float radius, int rings, int sectors) {
		List<Float> vertices = new ArrayList<>();
		List<Integer> indices = new ArrayList<>();

		float R = 1f / (float) (rings - 1);
		float S = 1f / (float) (sectors - 1);

		for (int r = 0; r < rings; r++) {
			for (int s = 0; s < sectors; s++) {
				float y = (float) Math.sin(-Math.PI / 2f + Math.PI * r * R);
				float x = (float) (Math.cos(2f * Math.PI * s * S) * Math.sin(Math.PI * r * R));
				float z = (float) (Math.sin(2f * Math.PI * s * S) * Math.sin(Math.PI * r * R));

				vertices.add(x * radius);
				vertices.add(y * radius);
				vertices.add(z * radius);
			}
		}

		for (int r = 0; r < rings - 1; r++) {
			for (int s = 0; s < sectors - 1; s++) {
				int current = r * sectors + s;
				int next = current + sectors;

				indices.add(current);
				indices.add(next);
				indices.add(current + 1);

				indices.add(current + 1);
				indices.add(next);
				indices.add(next + 1);
			}
		}

		float[] verticesArray = new float[vertices.size()];
		for (int i = 0; i < vertices.size(); i++) {
			verticesArray[i] = vertices.get(i);
		}

		int[] indicesArray = new int[indices.size()];
		for (int i = 0; i < indices.size(); i++) {
			indicesArray[i] = indices.get(i);
		}

		Vao vao = Vao.create();
		// vertices.size() içindeki toplam float sayısıdır. Nokta sayısı 3 float'a (x,y,z) eşittir.
		vao.storeData(indicesArray, vertices.size() / 3, verticesArray);
		return vao;
	}

}
