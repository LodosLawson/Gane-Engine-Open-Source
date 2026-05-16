package utils;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/**
 * OpenGL'in çeşitli durumlarını (state) performanslı bir şekilde yönetmek için yardımcı sınıf.
 * Gereksiz glEnable/glDisable çağrılarının önüne geçerek sistemin o anki durumunu önbelleğe (cache) alır.
 */
public class OpenGlUtils {
	
	// Geçerli durumları hafızada tutan değişkenler
	private static boolean cullingBackFace = false; // Arka yüz gizleme aktif mi?
	private static boolean inWireframe = false;     // Tel kafes çizim (Wireframe) aktif mi?
	private static boolean isAlphaBlending = false; // Normal saydamlık harmanlaması aktif mi?
	private static boolean additiveBlending = false;// Katkısal (additive) harmanlama aktif mi?
	private static boolean antialiasing = false;    // Kenar yumuşatma (Multisample) aktif mi?
	private static boolean depthTesting = false;    // Derinlik testi (Z-Buffer) aktif mi?

	/**
	 * Multisample (Kenar yumuşatma) modunu açar veya kapatır.
	 * @param enable true ise aç, false ise kapat
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
	 * Arkasındaki nesnelerle opaklık oranında karışım sağlar.
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
	 * Parlama efektleri (örn: Lens Flare, Güneş) için renkleri arkadaki piksellerin rengine ekler.
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
	 * Herhangi bir harmanlama (Blending) işlemi açıksa kapatır.
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
	 * @param enable true ise aç, false ise kapat
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
	 * Sadece kameraya dönük olan yüzeyleri çizerek performansı artırır.
	 * @param cull true ise aç, false ise kapat
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
	 * Modelleri içleri dolu değil de sadece çizgiler halinde (Tel Kafes / Wireframe) çizdirir.
	 * @param goWireframe true ise tel kafes modu, false ise normal katı (solid) mod
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
