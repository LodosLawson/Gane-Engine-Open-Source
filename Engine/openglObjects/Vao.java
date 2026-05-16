package openglObjects;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * VAO (Vertex Array Object) işlemlerini yöneten sarmalayıcı sınıf.
 * 3B modelin verilerini (vertex, doku UV, normal) barındıran VBO'ları ve indeks buffer'ını 
 * yapılandırıp bir araya getirerek OpenGL'e tanımlar.
 * Ayrıca verileri "interleaved" (birbiri ardına dizilmiş / araya serpiştirilmiş) formatta depolama yeteneğine sahiptir.
 */
public class Vao {
	
	// Bir float tipinin bellekte kapladığı bayt (byte) miktarı
	private static final int BYTES_PER_FLOAT = 4;

	// VAO'nun OpenGL tarafındaki eşsiz kimliği (ID)
	public final int id;
	// Tüm öznitelik (attribute) verilerini tutan tek parça VBO
	private Vbo dataVbo;
	// Noktaların çizim sırasını belirleyen (IBO/EBO) indeks VBO'su
	private Vbo indexVbo;
	// Toplam çizilecek indeks sayısı
	private int indexCount;

	/**
	 * Yeni bir Vertex Array Object (VAO) oluşturur.
	 * @return Oluşturulan VAO nesnesi
	 */
	public static Vao create() {
		int id = GL30.glGenVertexArrays();
		return new Vao(id);
	}

	private Vao(int id) {
		this.id = id;
	}
	
	/** @return Bu VAO'ya ait toplam indeks (çizilecek nokta) sayısı */
	public int getIndexCount(){
		return indexCount;
	}

	/** VAO'yu aktif hale getirir (bağlar). */
	public void bind() {
		GL30.glBindVertexArray(id);
	}
	
	/**
	 * VAO'yu ve belirtilen nitelik (attribute) listelerini (vertex, texture vb.) aktif hale getirir.
	 * Çizim işleminden hemen önce çağrılır.
	 * 
	 * @param attributes Aktifleştirilecek özellik indeksleri
	 */
	public void bind(int... attributes){
		bind();
		for (int i : attributes) {
			GL20.glEnableVertexAttribArray(i);
		}
	}

	/** VAO'yu pasif hale getirir (bağlantısını çözer). */
	public void unbind() {
		GL30.glBindVertexArray(0);
	}
	
	/**
	 * VAO'yu ve belirtilen nitelik listelerini pasif hale getirir.
	 * Çizim işlemi bittikten sonra çağrılır.
	 * 
	 * @param attributes Kapatılacak özellik indeksleri
	 */
	public void unbind(int... attributes){
		for (int i : attributes) {
			GL20.glDisableVertexAttribArray(i);
		}
		unbind();
	}
	
	/**
	 * Verilen indeks dizisi ve çoklu data dizilerini kullanarak VAO'ya verileri kaydeder.
	 * 
	 * @param indices Üçgenlerin nokta sırasını belirleyen indeks dizisi
	 * @param vertexCount Toplam nokta sayısı
	 * @param data Yüklenecek nitelik verileri (Vertex, Texture, Normal vs.)
	 */
	public void storeData(int[] indices, int vertexCount, float[]... data){
		bind();
		storeData(vertexCount, data);
		createIndexBuffer(indices);
		unbind();
	}
	
	/**
	 * VAO ve kendisine bağlı olan VBO'ları (data ve indeks) GPU belleğinden siler.
	 */
	public void delete() {
		GL30.glDeleteVertexArrays(id);
		dataVbo.delete();
		indexVbo.delete();
	}

	/**
	 * Farklı data dizilerini (vertex, uv, normal) birbirine örerek (interleave) tek bir VBO'ya kaydeder.
	 * Bu yöntem bellek okuma bant genişliğini (bandwidth) daha verimli kullanmayı sağlar (Cache dostudur).
	 */
	public void storeData(int vertexCount, float[]... data) {
		float[] interleavedData = interleaveFloatData(vertexCount, data);
		int[] lengths = getAttributeLengths(data, vertexCount);
		storeInterleavedData(interleavedData, lengths);
	}

	/**
	 * İndeks (Element Array) buffer'ını oluşturur ve VAO'ya bağlar.
	 */
	private void createIndexBuffer(int[] indices){
		this.indexVbo = Vbo.create(GL15.GL_ELEMENT_ARRAY_BUFFER);
		indexVbo.bind();
		indexVbo.storeData(indices);
		this.indexCount = indices.length;
	}
	
	/**
	 * Gelen her bir data setinin özellik başına düşen değer sayısını bulur.
	 * (Örn: Vertex verisi ise (x,y,z) olduğu için 3, UV ise (u,v) olduğu için 2 döner).
	 */
	private int[] getAttributeLengths(float[][] data, int vertexCount){
		int[] lengths = new int[data.length];
		for (int i = 0; i < data.length; i++) {
			lengths[i] = data[i].length / vertexCount;
		}
		return lengths;
	}

	/**
	 * İç içe geçirilmiş (interleaved) veriyi donanım belleğine yazar 
	 * ve özellik (attribute) pointer'larını yapılandırır.
	 */
	private void storeInterleavedData(float[] data, int... lengths) {
		dataVbo = Vbo.create(GL15.GL_ARRAY_BUFFER);
		dataVbo.bind();
		dataVbo.storeData(data);
		int bytesPerVertex = calculateBytesPerVertex(lengths);
		linkVboDataToAttributes(lengths, bytesPerVertex);
		dataVbo.unbind();
	}
	
	/**
	 * Shader'ın bu tekil VBO'nun içindeki verileri nasıl okuması gerektiğini (hangi aralıklarla atlayacağını) bildirir.
	 * (glVertexAttribPointer)
	 * 
	 * @param lengths Nitelik uzunlukları dizisi (örn: [3, 2, 3])
	 * @param bytesPerVertex Bir tam vertex bloğunun (vertex + uv + normal) bayt cinsinden uzunluğu
	 */
	private void linkVboDataToAttributes(int[] lengths, int bytesPerVertex){
		int total = 0;
		for (int i = 0; i < lengths.length; i++) {
			GL20.glVertexAttribPointer(i, lengths[i], GL11.GL_FLOAT, false, bytesPerVertex, BYTES_PER_FLOAT * total);
			total += lengths[i];
		}
	}
	
	/**
	 * Tüm veriler hesaplanırken tek bir Vertex'in donanımda kaplayacağı toplam bayt (Byte) alanını bulur (Stride).
	 */
	private int calculateBytesPerVertex(int[] lengths){
		int total = 0;
		for (int i = 0; i < lengths.length; i++) {
			total += lengths[i];
		}
		return BYTES_PER_FLOAT * total;
	}

	/**
	 * Verilen çoklu (ayrı) float dizilerini birbiri arasına sıkıştırarak 
	 * (örn: v1, v2, v3, uv1, uv2, n1, n2, n3...) tek ve düz bir float[] dizisi üretir.
	 * 
	 * @param count Nokta (vertex) sayısı
	 * @param data Birbirine örülecek ham nitelik listeleri
	 * @return İç içe geçirilmiş tek boyutlu dizi
	 */
	private float[] interleaveFloatData(int count, float[]... data) {
		int totalSize = 0;
		int[] lengths = new int[data.length];
		for (int i = 0; i < data.length; i++) {
			int elementLength = data[i].length / count;
			lengths[i] = elementLength;
			totalSize += data[i].length;
		}
		float[] interleavedBuffer = new float[totalSize];
		int pointer = 0;
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < data.length; j++) {
				int elementLength = lengths[j];
				for (int k = 0; k < elementLength; k++) {
					interleavedBuffer[pointer++] = data[j][i * elementLength + k];
				}
			}
		}
		return interleavedBuffer;
	}

}
