package openglObjects;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;

/**
 * VBO (Vertex Buffer Object) işlemlerini sarmalayan sınıf.
 * 3B modele ait vertex, texture, normal veya index gibi verileri ekran kartının (GPU) belleğine yüklemek için kullanılır.
 */
public class Vbo {
	
	// VBO'nun OpenGL tarafındaki eşsiz kimliği (ID)
	private final int vboId;
	// VBO tipi (örneğin: GL_ARRAY_BUFFER veya GL_ELEMENT_ARRAY_BUFFER)
	private final int type;
	
	/**
	 * Özel kurucu metod. Yeni bir VBO kimliği (id) ve tipiyle sınıfı ilklendirir.
	 */
	private Vbo(int vboId, int type){
		this.vboId = vboId;
		this.type = type;
	}
	
	/**
	 * Yeni bir VBO oluşturur.
	 * 
	 * @param type VBO tipi (örn: GL15.GL_ARRAY_BUFFER)
	 * @return Oluşturulan Vbo nesnesi
	 */
	public static Vbo create(int type){
		int id = GL15.glGenBuffers();
		return new Vbo(id, type);
	}
	
	/**
	 * Bu VBO'yu aktif hale getirir (bağlar).
	 */
	public void bind(){
		GL15.glBindBuffer(type, vboId);
	}
	
	/**
	 * Bu VBO'yu pasif hale getirir (bağlantısını çözer).
	 */
	public void unbind(){
		GL15.glBindBuffer(type, 0);
	}
	
	/**
	 * İlkel (primitive) float dizisini NIO buffer'a çevirerek GPU belleğine yazar.
	 * 
	 * @param data float dizisi
	 */
	public void storeData(float[] data){
		FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length);
		buffer.put(data);
		buffer.flip();
		storeData(buffer);
	}
	
	/**
	 * NIO FloatBuffer içerisindeki veriyi donanımın (GPU) belleğine (GL_STATIC_DRAW ile kalıcı olarak) yazar.
	 * 
	 * @param data NIO FloatBuffer
	 */
	public void storeData(FloatBuffer data){
		GL15.glBufferData(type, data, GL15.GL_STATIC_DRAW);
	}
	
	/**
	 * İlkel (primitive) int dizisini NIO buffer'a çevirerek GPU belleğine yazar.
	 * Genellikle indeks (EBO/IBO) buffer'ları için kullanılır.
	 * 
	 * @param data int dizisi
	 */
	public void storeData(int[] data){
		IntBuffer buffer = BufferUtils.createIntBuffer(data.length);
		buffer.put(data);
		buffer.flip();
		storeData(buffer);
	}
	
	/**
	 * NIO IntBuffer içerisindeki veriyi donanımın (GPU) belleğine yazar.
	 * 
	 * @param data NIO IntBuffer
	 */
	public void storeData(IntBuffer data){
		GL15.glBufferData(type, data, GL15.GL_STATIC_DRAW);
	}
	
	/**
	 * VBO'yu bellekten temizler ve OpenGL'den siler.
	 */
	public void delete(){
		GL15.glDeleteBuffers(vboId);
	}

}
