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
		if (data.getJointIds() != null && data.getVertexWeights() != null) {
			vao.storeData(data.getIndices(), data.getVertexCount(), data.getVertices(), data.getTextureCoords(),
					data.getNormals(), data.getJointIds(), data.getVertexWeights());
		} else {
			vao.storeData(data.getIndices(), data.getVertexCount(), data.getVertices(), data.getTextureCoords(),
					data.getNormals());
		}
		return new Model(vao, data);
	}

	/**
	 * Multi-Mesh destekli yükleyici. GLB dosyasındaki tüm alt parçaları (Mesh) ayrı modeller olarak yükler.
	 * 
	 * @param modelFile Yüklenecek dosya
	 * @return Yüklenmiş modellerin listesi
	 */
	public java.util.List<Model> loadModels(MyFile modelFile) {
		java.util.List<Model> models = new java.util.ArrayList<>();
		
		if (modelFile.getPath().toLowerCase().endsWith(".glb") || modelFile.getPath().toLowerCase().endsWith(".gld")) {
			java.util.List<ModelData> dataList = objConverter.GLBFileLoader.loadGLBModels(modelFile);
			for (ModelData data : dataList) {
				Vao vao = Vao.create();
				if (data.getJointIds() != null && data.getVertexWeights() != null) {
					vao.storeData(data.getIndices(), data.getVertexCount(), data.getVertices(), data.getTextureCoords(),
							data.getNormals(), data.getJointIds(), data.getVertexWeights());
				} else {
					vao.storeData(data.getIndices(), data.getVertexCount(), data.getVertices(), data.getTextureCoords(),
							data.getNormals());
				}
				models.add(new Model(vao, data));
			}
		} else {
			// OBJ dosyaları için (Tek model)
			models.add(loadModel(modelFile));
		}
		return models;
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
			data = new ModelData(vertices, data.getTextureCoords(), data.getNormals(), data.getIndices(), data.getJointIds(), data.getVertexWeights(), data.getFurthestPoint() * scale);
		}
		Vao vao = Vao.create();
		if (data.getJointIds() != null && data.getVertexWeights() != null) {
			vao.storeData(data.getIndices(), data.getVertexCount(), data.getVertices(), data.getTextureCoords(),
					data.getNormals(), data.getJointIds(), data.getVertexWeights());
		} else {
			vao.storeData(data.getIndices(), data.getVertexCount(), data.getVertices(), data.getTextureCoords(),
					data.getNormals());
		}
		return new Model(vao, data);
	}

}
