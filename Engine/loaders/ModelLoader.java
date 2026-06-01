package loaders;

import objConverter.ModelData;
import objConverter.OBJFileLoader;
import openglObjects.Vao;
import scene.Model;
import utils.MyFile;

/**
 * .obj formatındaki 3B dosyaları yükleyip, vertex verilerini OpenGL'e (VAO) kaydederek
 * oyun içinde kullanılabilecek Model objelerine dönüştürür.
 */
public class ModelLoader {

	/**
	 * Belirtilen .obj dosyasını okur, verilerini VAO'ya aktarır ve Model döndürür.
	 * 
	 * @param modelFile Yüklenecek obj dosyasının konumu
	 * @return Yüklenmiş model nesnesi
	 */
	public Model loadModel(MyFile modelFile) {
		ModelData data;
		if (modelFile.getPath().toLowerCase().endsWith(".glb") || modelFile.getPath().toLowerCase().endsWith(".gld")) {
			data = objConverter.GLBFileLoader.loadGLB(modelFile);
		} else {
			data = OBJFileLoader.loadOBJ(modelFile);
		}
		Vao vao = Vao.create();
		vao.storeData(data.getIndices(), data.getVertexCount(), data.getVertices(), data.getTextureCoords(),
				data.getNormals());
		return new Model(vao, data);
	}

	public Model loadModel(MyFile modelFile, float scale) {
		ModelData data;
		if (modelFile.getPath().toLowerCase().endsWith(".glb") || modelFile.getPath().toLowerCase().endsWith(".gld")) {
			data = objConverter.GLBFileLoader.loadGLB(modelFile);
		} else {
			data = OBJFileLoader.loadOBJ(modelFile);
		}
		if (scale != 1.0f) {
			float[] vertices = data.getVertices();
			for (int i = 0; i < vertices.length; i++) {
				vertices[i] *= scale;
			}
			data = new ModelData(vertices, data.getTextureCoords(), data.getNormals(), data.getIndices(), data.getFurthestPoint() * scale);
		}
		Vao vao = Vao.create();
		vao.storeData(data.getIndices(), data.getVertexCount(), data.getVertices(), data.getTextureCoords(),
				data.getNormals());
		return new Model(vao, data);
	}

}
