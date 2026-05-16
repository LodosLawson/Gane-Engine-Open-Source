package objConverter;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;

/**
 * 3B nesnedeki tek bir köşeyi (Vertex) temsil eden sınıf.
 * Pozisyon, doku indeksi, normal indeksi ve indeks konum bilgilerini barındırır.
 * OBJ yükleme aşamasında çakışan (aynı nokta ama farklı UV) indeksleri ayırmak için kullanılır.
 */
public class Vertex {
	
	// Henüz bir indeks atanmadığını belirten sabit değer
	private static final int NO_INDEX = -1;
	
	// Köşenin 3B uzaydaki (x,y,z) konumu
	private Vector3f position;
	// Köşeye ait doku (texture) bilgisinin indeksi
	private int textureIndex = NO_INDEX;
	// Köşeye ait normal bilgisinin indeksi
	private int normalIndex = NO_INDEX;
	// OpenGL aynı vertex'te farklı UV/Normal olmasını desteklemediği için
	// oluşturulan kopya (duplicate) vertex'in referansı
	private Vertex duplicateVertex = null;
	// Bu köşenin orijinal OBJ dosyasındaki veya listesindeki asıl indeksi
	private int index;
	// Başlangıç/merkez noktasına (0,0,0) olan uzaklığı
	private float length;
	
	// Yüzey teğet vektörleri listesi (Normal Mapping vb. gelişmiş ışıklandırmalar için)
	private List<Vector3f> tangents = new ArrayList<Vector3f>();
	// Hesaplanmış ortalama teğet vektörü
	private Vector3f averagedTangent = new Vector3f(0, 0, 0);
	
	/**
	 * Yeni bir köşe (vertex) oluşturur.
	 * 
	 * @param index Köşenin numarası/indeksi
	 * @param position Köşenin 3B konumu
	 */
	public Vertex(int index,Vector3f position){
		this.index = index;
		this.position = position;
		this.length = position.length();
	}
	
	/**
	 * Bu köşeye yeni bir teğet (tangent) vektörü ekler.
	 * 
	 * @param tangent Eklenecek teğet vektörü
	 */
	public void addTangent(Vector3f tangent){
		tangents.add(tangent);
	}
	
	/**
	 * Bu köşeye eklenen tüm teğet vektörlerinin ortalamasını alıp normalleştirir.
	 * Pürüzsüzleştirilmiş teğet hesabı için kullanılır.
	 */
	public void averageTangents(){
		if(tangents.isEmpty()){
			return;
		}
		for(Vector3f tangent : tangents){
			Vector3f.add(averagedTangent, tangent, averagedTangent);
		}
		averagedTangent.normalise();
	}
	
	/** @return Hesaplanmış ortalama teğet vektörünü döndürür */
	public Vector3f getAverageTangent(){
		return averagedTangent;
	}
	
	/** @return Köşenin indeksini döndürür */
	public int getIndex(){
		return index;
	}
	
	/** @return Merkeze olan uzaklığını döndürür */
	public float getLength(){
		return length;
	}
	
	/** @return Doku ve normal indeksleri belirlenmiş mi (set edilmiş mi)? */
	public boolean isSet(){
		return textureIndex!=NO_INDEX && normalIndex!=NO_INDEX;
	}
	
	/**
	 * Belirtilen doku ve normal indekslerinin bu vertex'tekilerle uyuşup uyuşmadığını test eder.
	 * 
	 * @param textureIndexOther Diğer doku indeksi
	 * @param normalIndexOther Diğer normal indeksi
	 * @return İndeksler tamamen aynıysa true
	 */
	public boolean hasSameTextureAndNormal(int textureIndexOther,int normalIndexOther){
		return textureIndexOther==textureIndex && normalIndexOther==normalIndex;
	}
	
	/** Doku indeksini ayarlar */
	public void setTextureIndex(int textureIndex){
		this.textureIndex = textureIndex;
	}
	
	/** Normal indeksini ayarlar */
	public void setNormalIndex(int normalIndex){
		this.normalIndex = normalIndex;
	}

	/** @return 3B konumu (pozisyonu) döndürür */
	public Vector3f getPosition() {
		return position;
	}

	/** @return Doku indeksini döndürür */
	public int getTextureIndex() {
		return textureIndex;
	}

	/** @return Normal indeksini döndürür */
	public int getNormalIndex() {
		return normalIndex;
	}

	/** @return Çakışmayı önlemek için yaratılan kopya vertex objesini döndürür */
	public Vertex getDuplicateVertex() {
		return duplicateVertex;
	}

	/** Çakışmayı önlemek için yaratılan kopya vertex objesini atar */
	public void setDuplicateVertex(Vertex duplicateVertex) {
		this.duplicateVertex = duplicateVertex;
	}

}
