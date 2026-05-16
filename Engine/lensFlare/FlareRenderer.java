package lensFlare;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.util.vector.Vector2f;

import openglObjects.Query;
import openglObjects.Vao;
import utils.OpenGlUtils;

/**
 * Ekran üzerine 2 Boyutlu dokulanmış (textured) dörtgenler (quad) çizer.
 * Mercek parlamalarının render edilmesinden sorumludur.
 * 
 * @author Karl
 */
public class FlareRenderer {

	// 2D dörtgen için 4 köşenin (vertex) pozisyonları.
	private static final float[] POSITIONS = { -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f, -0.5f };

	// Güneşin arkada kalıp kalmadığını kontrol etmek için yapılan test dörtgeninin boyutları
	private static final float TEST_QUAD_WIDTH = 0.07f;
	private static final float TEST_QUAD_HEIGHT = TEST_QUAD_WIDTH * (float) Display.getWidth() / Display.getHeight();
	// Test sırasında oluşacak maksimum sample miktarı
	private static final float TOTAL_SAMPLES = (float) Math.pow(TEST_QUAD_WIDTH * Display.getWidth() * 0.5f, 2) * 4;

	// Dörtgenin pozisyonlarını 0 numaralı attribute içinde tutan VAO (Vertex Array Object)
	private final Vao quad;
	// Parlama shader programı
	private final FlareShader shader;
	// OpenGL Occlusion Query objesi: Güneşin kaç pikselinin ekranda göründüğünü hesaplar
	private final Query query;

	// Parlaklık kapsamı (0.0 hiç görünmüyor, 1.0 tamamen görünüyor)
	private float coverage = 0;
	
	/**
	 * Shader programını ilklendirir, dörtgen için VAO oluşturur ve
	 * 4 dörtgen köşesi bilgisini VAO'nun 0. attribute'una kaydeder.
	 */
	public FlareRenderer() {
		this.shader = new FlareShader();
		this.query = new Query(GL15.GL_SAMPLES_PASSED);
		this.quad = Vao.create();
		quad.bind();
		quad.storeData(4, POSITIONS);
		quad.unbind();
	}

	/**
	 * Belirtilen parlaklıkta FlareTexture'ları ekran konumlarına göre render eder.
	 * 
	 * @param sunScreenPos Güneşin ekrandaki x,y pozisyonu
	 * @param flares       Ekrana çizilecek olan FlareTexture'ların bir dizisi
	 * @param brightness   Bütün FlareTexture'ların alacağı temel parlaklık değeri
	 */
	public void render(Vector2f sunScreenPos, FlareTexture[] flares, float brightness) {
		prepare(brightness);
		// Güneşin herhangi bir objenin (örn: dağların) arkasında kalıp kalmadığını test et
		doOcclusionTest(sunScreenPos);
		// Eklemeli harmanlama (additive blending) aktif et
		OpenGlUtils.enableAdditiveBlending();
		// Parlamalar her zaman önde çizileceği için derinlik testini kapat
		OpenGlUtils.enableDepthTesting(false);
		for (FlareTexture flare : flares) {
			renderFlare(flare);
		}
		endRendering();
	}

	/**
	 * Donanım düzeyinde, güneşin ekrandaki alanda herhangi bir şeyin (model, arazi vb.)
	 * arkasında kalıp kalmadığını kontrol eder.
	 * 
	 * @param sunScreenCoords Güneşin ekran koordinatları
	 */
	private void doOcclusionTest(Vector2f sunScreenCoords) {
		if(query.isResultReady()){
			int visibleSamples = query.getResult();
			this.coverage = Math.min(visibleSamples / TOTAL_SAMPLES, 1f);
		}
		if (!query.isInUse()) {
			// Sadece derinlik testi yapacağız, ekrana renk çizmeyi ve derinliği değiştirmeyi kapat
			GL11.glColorMask(false, false, false, false);
			GL11.glDepthMask(false);
			query.start();
			OpenGlUtils.enableDepthTesting(true);
			shader.transform.loadVec4(sunScreenCoords.x, sunScreenCoords.y, TEST_QUAD_WIDTH, TEST_QUAD_HEIGHT);
			GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
			query.end();
			// Renk maskelerini ve derinlik maskesini geri aktif et
			GL11.glColorMask(true, true, true, true);
			GL11.glDepthMask(true);
		}
	}

	/**
	 * Shader programını temizler. Uygulama kapandığında kullanılmalıdır.
	 */
	public void cleanUp() {
		query.delete();
		shader.cleanUp();
	}

	/**
	 * FlareTexture'ları çizmek için gerekli ön hazırlıkları yapar.
	 * 
	 * Antialiasing'e gerek olmadığı için kapatılır. Eklemeli harmanlama (Additive blending) 
	 * açılır; böylece dokunun şeffaf yerleri gözükmez ve dokunun geneline "parlayan" bir
	 * görünüm verilir. Derinlik testi (depth testing) devre dışı bırakılır çünkü parlama dokuları
	 * sahnede her şeyin önüne çizilmelidir. Bu nedenle mercek parlaması (lens flare) efekti
	 * 3B sahnedeki her şey çizildikten SONRA çağrılmalıdır. (Eğer GUI'lerin parlamanın önüne
	 * geçmesini istiyorsanız parlamayı GUI'lerden ÖNCE çizmelisiniz.)
	 * Arka yüzey gizlemeye (backface culling) bu işlemde gerek yoktur.
	 * 
	 * Shader programı başlatılır ve parlaklık değeri (brightness) uniform olarak shader'a yollanır.
	 * Dörtgenin VAO'su bağlanarak kullanıma hazır hale getirilir.
	 * 
	 * @param brightness Parlamaların çizileceği temel parlaklık seviyesi.
	 */
	private void prepare(float brightness) {
		OpenGlUtils.antialias(false);
		shader.start();
		// Parlaklık = hesaplanan baz parlaklık x görünürlük oranı
		shader.brightness.loadFloat(brightness * coverage);
		quad.bind(0);
	}

	/**
	 * Tek bir parlama dokusunu ekrandaki kaplamalı (textured) 2 boyutlu dörtgen üzerine çizer.
	 * 
	 * Dokunun 0. doku birimine (texture unit 0) bağlanmasıyla başlar. Dörtgenin x ve y
	 * ölçekleri belirlenir. x ölçeği sadece FlareTexture'daki scale değeri iken,
	 * y ölçeği bu değerin ekranın en/boy oranıyla (aspect ratio) çarpılmasıyla hesaplanır.
	 * Böylece dörtgenin dikdörtgen değil tam bir kare olması sağlanır.
	 * 
	 * Sonrasında konum ve ölçek shader'a yüklenir. Son olarak dörtgen, glDrawArrays kullanılarak
	 * (indeks tamponu kullanılmadan) ve GL_TRIANGLE_STRIP kullanılarak çizilir. Bu teknik sayesinde
	 * 2 üçgen için 6 köşe belirlemek yerine, sadece 4 köşe ile dörtgen tanımlanabilmektedir.
	 * 
	 * @param flare Çizilecek olan parlama objesi.
	 */
	private void renderFlare(FlareTexture flare) {
		flare.getTexture().bindToUnit(0);
		float xScale = flare.getScale();
		float yScale = xScale * (float) Display.getWidth() / Display.getHeight();
		Vector2f centerPos = flare.getScreenPos();
		shader.transform.loadVec4(centerPos.x, centerPos.y, xScale, yScale);
		GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
	}

	/**
	 * Dörtgenin VAO bağını çözer, shader programını durdurur ve çizim öncesi değiştirilen
	 * ayarlamaları eski haline getirir.
	 */
	private void endRendering() {
		quad.unbind(0);
		shader.stop();
		OpenGlUtils.disableBlending();
		OpenGlUtils.enableDepthTesting(true);
	}

}
