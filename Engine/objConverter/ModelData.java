package objConverter;

/**
 * 3B modelin ayrıştırılmış verilerini bellekte tutan sarmalayıcı (wrapper) sınıf.
 * Dizi (array) formundaki köşe, doku, normal koordinatları ve bağlantı indekslerini içerir.
 */
public class ModelData {
	
	// Uzay boyutu (X, Y, Z için 3 boyut)
	private static final int DIMENSIONS = 3;

	// Modelin köşe noktalarının (vertex) uzaydaki koordinat dizisi
	private float[] vertices;
	// Doku (texture UV) koordinat dizisi
	private float[] textureCoords;
	// Yüzey normalleri dizisi (ışıklandırma hesapları için)
	private float[] normals;
	// Üçgenlerin hangi noktalardan oluştuğunu tutan indeks dizisi
	private int[] indices;
	// Modelin merkezden en uzak noktasının mesafesi (culling/kırpma hesapları için)
	private float furthestPoint;

	/**
	 * Model verilerini ilklendirir.
	 * 
	 * @param vertices Köşe noktası dizisi
	 * @param textureCoords Doku koordinatı dizisi
	 * @param normals Normal vektör dizisi
	 * @param indices Nokta bağlantı indeks dizisi
	 * @param furthestPoint Merkezden en uzak noktanın mesafesi
	 */
	public ModelData(float[] vertices, float[] textureCoords, float[] normals, int[] indices,
			float furthestPoint) {
		this.vertices = vertices;
		this.textureCoords = textureCoords;
		this.normals = normals;
		this.indices = indices;
		this.furthestPoint = furthestPoint;
	}
	
	/**
	 * @return Toplam köşe noktası (vertex) sayısını döndürür. (Toplam dizi uzunluğunun boyuta bölümü)
	 */
	public int getVertexCount(){
		return vertices.length/DIMENSIONS;
	}

	/** @return Köşe noktası koordinatlarını döndürür */
	public float[] getVertices() {
		return vertices;
	}

	/** @return Doku koordinatlarını döndürür */
	public float[] getTextureCoords() {
		return textureCoords;
	}

	/** @return Yüzey normallerini döndürür */
	public float[] getNormals() {
		return normals;
	}

	/** @return Üçgenleri oluşturan indeksleri döndürür */
	public int[] getIndices() {
		return indices;
	}

	/** @return Merkezden en uzak noktanın mesafesini döndürür */
	public float getFurthestPoint() {
		return furthestPoint;
	}

}
