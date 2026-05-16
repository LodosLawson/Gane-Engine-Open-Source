package scene;

import openglObjects.Vao;

/**
 * 3B nesnelerin GPU belleğindeki geometri verisini temsil eden sınıf.
 * Temel olarak bir VAO'yu (Vertex Array Object) sarar.
 */
public class Model {
	
	// Modelin OpenGL tarafındaki kimliği ve verilerini tutan VAO
	private final Vao vao;
	
	/**
	 * Yeni bir 3B model oluşturur.
	 * 
	 * @param vao Modelin vertex/normal/texture koordinatlarını tutan VAO
	 */
	public Model(Vao vao){
		this.vao = vao;
	}
	
	/** @return Modelin bağlı olduğu VAO nesnesini döndürür */
	public Vao getVao(){
		return vao;
	}
	
	/** Modelin kullandığı VAO'yu bellekten tamamen siler */
	public void delete(){
		vao.delete();
	}

}
