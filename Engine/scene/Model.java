package scene;

import openglObjects.Vao;

/**
 * 3B nesnelerin GPU belleğindeki geometri verisini temsil eden sınıf.
 * Temel olarak bir VAO'yu (Vertex Array Object) sarar.
 */
public class Model {
	
	// Modelin OpenGL tarafındaki kimliği ve verilerini tutan VAO
	private final Vao vao;
	
	// Orijinal verileri (köşeler, dokular vs.) CPU tarafında tutar (Static Batching için)
	private objConverter.ModelData modelData;
	
	/**
	 * Yeni bir 3B model oluşturur. (Eski uyumluluk)
	 * 
	 * @param vao Modelin vertex/normal/texture koordinatlarını tutan VAO
	 */
	public Model(Vao vao){
		this.vao = vao;
	}

	/**
	 * Yeni bir 3B model oluşturur ve orijinal ModelData'yı saklar.
	 * 
	 * @param vao Modelin vertex/normal/texture koordinatlarını tutan VAO
	 * @param modelData CPU tarafındaki ham köşeler, doku koordinatları ve normaller
	 */
	public Model(Vao vao, objConverter.ModelData modelData) {
		this.vao = vao;
		this.modelData = modelData;
	}
	
	/** @return Modelin bağlı olduğu VAO nesnesini döndürür */
	public Vao getVao(){
		return vao;
	}
	
	/** @return Modelin orijinal ham verilerini (ModelData) döndürür (null olabilir) */
	public objConverter.ModelData getModelData() {
		return modelData;
	}
	
	/** Modelin kullandığı VAO'yu bellekten tamamen siler */
	public void delete(){
		vao.delete();
	}

}
