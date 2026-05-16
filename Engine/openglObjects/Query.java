package openglObjects;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

/**
 * Donanım tabanlı asenkron OpenGL sorgularını (Occlusion Query vb.) yöneten sınıf.
 * Genellikle GPU üzerinde bir işlemin sonucunu (örn: kaç piksel çizildiğini) öğrenmek için kullanılır.
 */
public class Query {
	
	// Sorgunun OpenGL tarafındaki eşsiz ID'si
	private final int id;
	// Sorgunun tipi (örneğin: GL_SAMPLES_PASSED, GL_ANY_SAMPLES_PASSED)
	private final int type;
	
	// Sorgunun aktif olarak bir şeyleri ölçüp ölçmediğini tutan bayrak
	private boolean inUse = false;
	
	/**
	 * Belirtilen tipte yeni bir donanım sorgusu oluşturur.
	 * 
	 * @param type Sorgu tipi (örn: GL15.GL_SAMPLES_PASSED)
	 */
	public Query(int type){
		this.type = type;
		this.id = GL15.glGenQueries();
	}
	
	/**
	 * Sorguyu başlatır. Bu noktadan sonra yapılan çizimler sorgu için kayıt altına alınır.
	 */
	public void start(){
		GL15.glBeginQuery(type, id);
		inUse = true;
	}
	
	/**
	 * Sorguyu sonlandırır. Ölçüm tamamlanır ve GPU'dan sonuç beklenir.
	 */
	public void end(){
		GL15.glEndQuery(type);
	}
	
	/**
	 * Asenkron çalışan GPU sorgusunun tamamlanıp tamamlanmadığını kontrol eder.
	 * Programı bloklamamak (bekletmemek) için sonucu almadan önce bu kontrol yapılmalıdır.
	 * 
	 * @return Sonuç hazırsa true, işlem devam ediyorsa false
	 */
	public boolean isResultReady(){
		return GL15.glGetQueryObjecti(id, GL15.GL_QUERY_RESULT_AVAILABLE) == GL11.GL_TRUE;
	}
	
	/**
	 * @return Bu sorgu şu anda ölçüm işlemi yapıyor mu?
	 */
	public boolean isInUse(){
		return inUse;
	}
	
	/**
	 * Sorgu sonucunu döndürür (örneğin derinlik testini geçen piksel sayısı).
	 * DİKKAT: Eğer isResultReady() true dönmeden çağrılırsa programı GPU bitirene kadar bloklayabilir.
	 * 
	 * @return Sorgunun sonucu
	 */
	public int getResult(){
		inUse = false;
		return GL15.glGetQueryObjecti(id, GL15.GL_QUERY_RESULT);
	}
	
	/**
	 * Sorguyu OpenGL belleğinden siler.
	 */
	public void delete(){
		GL15.glDeleteQueries(id);
	}

}
