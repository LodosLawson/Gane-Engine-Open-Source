package sunRenderer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import openglObjects.Vao;
import utils.ICamera;
import utils.OpenGlUtils;

/**
 * Güneş objesini ekrana çizen (render eden) özel sınıf.
 * Güneşi, her zaman kameraya dönük olan (billboarding tekniği) bir dörtgen (quad) üzerine çizer.
 */
public class SunRenderer {

	private final SunShader shader;

	// Güneşi çizmek için kullanılacak basit dörtgenin (quad) köşe koordinatları (x,y)
	private static final float[] POSITIONS = { -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f, -0.5f };

	// Dörtgen geometrisini tutan VAO nesnesi
	private final Vao quad;

	/**
	 * SunRenderer'ı başlatır. Shader yüklenir ve dörtgen (quad) VAO'su GPU'ya kaydedilir.
	 */
	public SunRenderer() {
		this.shader = new SunShader();
		this.quad = Vao.create();
		quad.bind();
		// Koordinatları VAO'ya yükle
		quad.storeData(4, POSITIONS);
		quad.unbind();
	}

	/**
	 * Güneşi kameranın bakış açısına göre ekrana çizer.
	 * 
	 * @param sun Çizilecek Güneş nesnesi
	 * @param camera Oyuncu kamerası
	 */
	public void render(Sun sun, ICamera camera) {
		prepare(sun, camera);
		// Dörtgeni (quad) triangle strip (üçgen şeridi) olarak çiz
		GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
		endRendering();
	}

	/**
	 * Renderer kapatılırken Güneş shader'ını bellekten siler.
	 */
	public void cleanUp() {
		shader.cleanUp();
	}

	/**
	 * Çizim öncesi OpenGL durumlarını (state) hazırlar.
	 * Güneşin arka planla karışması için blending (harmanlama) açılır.
	 * 
	 * @param sun Çizilecek güneş nesnesi
	 * @param camera Oyuncu kamerası
	 */
	private void prepare(Sun sun, ICamera camera) {
		OpenGlUtils.antialias(false);
		// Güneş gökyüzünün arkasında/içinde olduğu için derinlik tamponuna yazılmasını engelliyoruz
		GL11.glDepthMask(false);
		// Güneş kaplamasının şeffaflık (alpha) ayarlarının çalışması için harmanlamayı aktif ediyoruz
		OpenGlUtils.enableAlphaBlending();
		shader.start();
		
		// Model-View-Projection matrisini hesapla ve shader'a yükle
		Matrix4f mvpMat = calculateMvpMatrix(sun, camera);
		shader.mvpMatrix.loadMatrix(mvpMat);
		
		quad.bind(0);
		// Güneşin kaplamasını 0 numaralı doku birimine bağla
		sun.getTexture().bindToUnit(0);
	}

	/**
	 * Güneşin ekrandaki nihai pozisyonunu belirleyen Model-View-Projection (MVP) matrisini hesaplar.
	 * 
	 * @param sun Güneş nesnesi
	 * @param camera Kamera nesnesi
	 * @return Hesaplanmış MVP matrisi
	 */
	private Matrix4f calculateMvpMatrix(Sun sun, ICamera camera) {
		Matrix4f modelMatrix = new Matrix4f();
		Vector3f sunPos = sun.getWorldPosition(camera.getPosition());
		// Güneşi ilgili pozisyona ötele (translate)
		Matrix4f.translate(sunPos, modelMatrix, modelMatrix);
		
		// Billboarding işlemi (Güneşin her zaman kameraya bakması) için view matrisini uygula
		Matrix4f modelViewMat = applyViewMatrix(modelMatrix, camera.getViewMatrix());
		
		// Güneşi ölçeklendir (büyült/küçült)
		Matrix4f.scale(new Vector3f(sun.getScale(), sun.getScale(), sun.getScale()), modelViewMat, modelViewMat);
		
		// Projeksiyon matrisi ile çarparak nihai MVP matrisini elde et
		return Matrix4f.mul(camera.getProjectionMatrix(), modelViewMat, null);
	}

	/**
	 * Parçacık (Particle) sistemi eğitimlerindeki mantık ile aynıdır.
	 * Temel olarak görünüm (View) matrisinin dönüş (rotation) etkisini iptal ederiz.
	 * Böylece Güneşin çizildiği dörtgen (quad) 3 boyutlu uzayda nerede olursa olsun her zaman tam karşıdan kameraya bakar.
	 * (Bu tekniğe 'Billboarding' denir).
	 * 
	 * @param modelMatrix Güneşin dünya üzerindeki konumunu tutan model matrisi
	 * @param viewMatrix Kameranın görünüm matrisi
	 * @return Dönüş (Rotation) etkisi sıfırlanmış Model-View matrisi.
	 */
	private Matrix4f applyViewMatrix(Matrix4f modelMatrix, Matrix4f viewMatrix) {
		// Model matrisinin dönüş kısımlarını, View matrisinin dönüş kısımlarının tersi/aynısı ile değiştirerek
		// objenin kameraya sabit bakmasını sağlıyoruz.
		modelMatrix.m00 = viewMatrix.m00;
		modelMatrix.m01 = viewMatrix.m10;
		modelMatrix.m02 = viewMatrix.m20;
		modelMatrix.m10 = viewMatrix.m01;
		modelMatrix.m11 = viewMatrix.m11;
		modelMatrix.m12 = viewMatrix.m21;
		modelMatrix.m20 = viewMatrix.m02;
		modelMatrix.m21 = viewMatrix.m12;
		modelMatrix.m22 = viewMatrix.m22;
		return Matrix4f.mul(viewMatrix, modelMatrix, null);
	}

	/**
	 * Çizim bittikten sonra OpenGL durumlarını (state) eski haline getirir.
	 */
	private void endRendering() {
		// Derinlik tamponuna yazmayı tekrar aktif et
		GL11.glDepthMask(true);
		quad.unbind(0);
		shader.stop();
		// Harmanlamayı (Blending) kapat
		OpenGlUtils.disableBlending();
	}

}
