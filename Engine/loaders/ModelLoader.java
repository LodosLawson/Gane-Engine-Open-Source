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
		ModelData data = OBJFileLoader.loadOBJ(modelFile);
		Vao vao = Vao.create();
		vao.storeData(data.getIndices(), data.getVertexCount(), data.getVertices(), data.getTextureCoords(),
				data.getNormals());
		return new Model(vao);
	}

}
