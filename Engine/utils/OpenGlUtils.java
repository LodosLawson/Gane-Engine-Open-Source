package utils;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/**
 * OpenGL'in çeşitli durumlarını (state) performanslı bir şekilde yönetmek için oluşturulmuş yardımcı sınıf.
 * Gereksiz glEnable/glDisable çağrılarının önüne geçerek sistemin o anki durumunu önbellekte (cache) tutar.
 * Ekran kartına sadece gerçekten bir durum değiştiğinde komut göndererek performansı (FPS) artırır.
 * 
 * <p><b>Kullanım Örneği:</b></p>
 * <pre>
 * {@code
 * // Saydam objeleri (su, cam vb.) çizmeden önce harmanlamayı aç ve arka yüzleri çizmeyi kapat
 * OpenGlUtils.enableAlphaBlending();
 * OpenGlUtils.cullBackFaces(false);
 * 
 * renderer.renderTransparentObjects();
 * 
 * // Tekrar varsayılan ayarlara dön
 * OpenGlUtils.disableBlending();
 * OpenGlUtils.cullBackFaces(true);
 * }
 * </pre>
 */
public class OpenGlUtils {
	
	/** Arka yüz gizleme (Back-face Culling) aktif mi? */
	private static boolean cullingBackFace = false; 
	/** Tel kafes çizim (Wireframe Mode) aktif mi? */
	private static boolean inWireframe = false;     
	/** Normal saydamlık harmanlaması (Alpha Blending) aktif mi? */
	private static boolean isAlphaBlending = false; 
	/** Katkısal harmanlama (Additive Blending) aktif mi? */
	private static boolean additiveBlending = false;
	/** Kenar yumuşatma (Multisampling / Antialiasing) aktif mi? */
	private static boolean antialiasing = false;    
	/** Derinlik testi (Z-Buffer Depth Test) aktif mi? */
	private static boolean depthTesting = false;    

	/**
	 * Multisample (Kenar yumuşatma - Antialiasing) modunu açar veya kapatır.
	 * Tırtıklı kenarları pürüzsüzleştirir.
	 * 
	 * @param enable true ise kenar yumuşatmayı aç, false ise kapat.
	 */
	public static void antialias(boolean enable) {
		if (enable && !antialiasing) {
			GL11.glEnable(GL13.GL_MULTISAMPLE);
			antialiasing = true;
		} else if (!enable && antialiasing) {
			GL11.glDisable(GL13.GL_MULTISAMPLE);
			antialiasing = false;
		}
	}

	/**
	 * Normal saydamlık (Alpha Blending) harmanlamasını açar.
	 * Çizilen objenin saydamlık (alpha) değerine göre arkasındaki nesnelerle renkleri karıştırır.
	 * Cam, su veya yarı saydam parçacıklar için kullanılır.
	 */
	public static void enableAlphaBlending() {
		if (!isAlphaBlending) {
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			isAlphaBlending = true;
			additiveBlending = false;
		}
	}

	/**
	 * Katkısal harmanlamayı (Additive Blending) açar.
	 * Çizilen nesnenin renk değerlerini, arkasında kalan piksellerin rengine doğrudan toplar (ekler).
	 * Parlama efektleri, ateş, lazer, lens flare veya büyü efektleri için kullanılır.
	 */
	public static void enableAdditiveBlending() {
		if (!additiveBlending) {
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
			additiveBlending = true;
			isAlphaBlending = false;
		}
	}

	/**
	 * Herhangi bir harmanlama (Alpha veya Additive Blending) işlemi açıksa kapatır.
	 * Katı (saydam olmayan) objeleri çizmeden önce çağrılmalıdır.
	 */
	public static void disableBlending() {
		if (isAlphaBlending || additiveBlending) {
			GL11.glDisable(GL11.GL_BLEND);
			isAlphaBlending = false;
			additiveBlending = false;
		}
	}
	
	/**
	 * Derinlik testini (Z-Buffer depth test) açar veya kapatır.
	 * Açık olduğunda uzaktaki objelerin, yakındaki objelerin önüne çizilmesini engeller.
	 * 
	 * @param enable true ise derinlik testini aç, false ise kapat.
	 */
	public static void enableDepthTesting(boolean enable){
		if(enable && !depthTesting){
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			depthTesting = true;
		}else if(!enable && depthTesting){
			GL11.glDisable(GL11.GL_DEPTH_TEST);
			depthTesting = false;
		}
	}

	/**
	 * Arka yüz gizlemeyi (Back-face Culling) açar veya kapatır.
	 * Sadece kameraya dönük olan yüzeyleri çizerek işlenecek poligon sayısını yarıya indirir 
	 * ve performansı artırır. Ancak iki tarafı da görünmesi gereken ince objeler (örn: çimen, yaprak)
	 * çizilecekse geçici olarak kapatılmalıdır.
	 * 
	 * @param cull true ise arka yüzleri gizle (performans artar), false ise her iki yüzü de çiz.
	 */
	public static void cullBackFaces(boolean cull) {
		if (cull && !cullingBackFace) {
			GL11.glEnable(GL11.GL_CULL_FACE);
			GL11.glCullFace(GL11.GL_BACK);
			cullingBackFace = true;
		} else if (!cull && cullingBackFace) {
			GL11.glDisable(GL11.GL_CULL_FACE);
			cullingBackFace = false;
		}
	}

	/**
	 * Modelleri içleri dolu (solid) olarak değil de sadece çizgiler halinde (Tel Kafes / Wireframe) çizdirir.
	 * Genellikle hata ayıklama (debug) veya hit-box gösterimi için kullanılır.
	 * 
	 * @param goWireframe true ise tel kafes moduna geç, false ise normal (dolu) katı çizim moduna dön.
	 */
	public static void goWireframe(boolean goWireframe) {
		if (goWireframe && !inWireframe) {
			GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
			inWireframe = true;
		} else if (!goWireframe && inWireframe) {
			GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
			inWireframe = false;
		}
	}

}
