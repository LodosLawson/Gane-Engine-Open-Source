package environmentMapRenderer;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import utils.ICamera;

/**
 * Küp haritası (Cube Map) çizimi yapabilmek için kullanılan özel bir kameradır.
 * Çevresel yansımaları hesaplarken, merkezden dışarı doğru 6 yöne (küpün yüzeylerine) bakmak için kullanılır.
 */
public class CubeMapCamera implements ICamera {

	/**
	 * Kameranın çizim yapabileceği en yakın mesafe.
	 */
	private static final float NEAR_PLANE = 0.1f;
	
	/**
	 * Kameranın çizim yapabileceği en uzak mesafe.
	 */
	private static final float FAR_PLANE = 200f;
	
	/**
	 * Görüş açısı (Field of View). Küp yüzeyleri çizildiği için 90 derece olması zorunludur.
	 */
	private static final float FOV = 90;// don't change!
	
	/**
	 * Ekran oranı. Küp yüzeyleri kare olduğu için 1 olmalıdır.
	 */
	private static final float ASPECT_RATIO = 1;

	/**
	 * Kameranın 3D uzaydaki merkez pozisyonu.
	 */
	private final Vector3f center;
	
	/**
	 * Kameranın yukarı-aşağı bakış açısı.
	 */
	private float pitch = 0;
	
	/**
	 * Kameranın sağa-sola dönüş açısı.
	 */
	private float yaw = 0;

	/**
	 * Perspektif matrisi.
	 */
	private Matrix4f projectionMatrix = new Matrix4f();
	
	/**
	 * Görüş matrisi. Kameranın pozisyonu ve açısını temsil eder.
	 */
	private Matrix4f viewMatrix = new Matrix4f();
	
	/**
	 * Perspektif ve görüş matrisinin birleşimi.
	 */
	private Matrix4f projectionViewMatrix = new Matrix4f();

	/**
	 * Yapıcı metot. Kamerayı bir merkez noktasına göre oluşturur ve perspektif matrisini hesaplar.
	 * @param center Kameranın konumu.
	 */
	public CubeMapCamera(Vector3f center) {
		this.center = center;
		createProjectionMatrix();
	}

	/**
	 * Kamerayı küpün belirtilen yüzeyine doğru çevirir.
	 * Neden: Çevresel haritayı oluştururken kameranın 6 farklı yöne sırayla bakması ve her yönün resmini çekmesi gerekir.
	 * @param faceIndex Bakılacak yüzeyin indeksi (0-5 arası).
	 */
	public void switchToFace(int faceIndex) {
		switch (faceIndex) {
		case 0:
			pitch = 0;
			yaw = 90;
			break;
		case 1:
			pitch = 0;
			yaw = -90;
			break;
		case 2:
			pitch = -90;
			yaw = 180;
			break;
		case 3:
			pitch = 90;
			yaw = 180;
			break;
		case 4:
			pitch = 0;
			yaw = 180;
			break;
		case 5:
			pitch = 0;
			yaw = 0;
			break;
		}
		updateViewMatrix();
	}

	@Override
	public Vector3f getPosition() {
		return center;
	}

	@Override
	public Matrix4f getViewMatrix() {
		return viewMatrix;
	}

	@Override
	public void reflect(float height) {
		// Çevresel kamera yansıma fonksiyonunu şimdilik kullanmıyor.
	}

	@Override
	public Matrix4f getProjectionMatrix() {
		return projectionMatrix;
	}

	@Override
	public Matrix4f getProjectionViewMatrix() {
		return projectionViewMatrix;
	}

	/**
	 * Kameranın perspektif (görüş mesafesi ve genişliği) matrisini oluşturur.
	 */
	private void createProjectionMatrix() {
		float y_scale = (float) ((1f / Math.tan(Math.toRadians(FOV / 2f))));
		float x_scale = y_scale / ASPECT_RATIO;
		float frustum_length = FAR_PLANE - NEAR_PLANE;

		projectionMatrix.m00 = x_scale;
		projectionMatrix.m11 = y_scale;
		projectionMatrix.m22 = -((FAR_PLANE + NEAR_PLANE) / frustum_length);
		projectionMatrix.m23 = -1;
		projectionMatrix.m32 = -((2 * NEAR_PLANE * FAR_PLANE) / frustum_length);
		projectionMatrix.m33 = 0;
	}

	/**
	 * Kameranın pozisyon ve açısına göre görüş (view) matrisini günceller.
	 */
	private void updateViewMatrix() {
		viewMatrix.setIdentity();
		Matrix4f.rotate((float) Math.toRadians(180), new Vector3f(0, 0, 1), viewMatrix, viewMatrix);
		Matrix4f.rotate((float) Math.toRadians(pitch), new Vector3f(1, 0, 0), viewMatrix, viewMatrix);
		Matrix4f.rotate((float) Math.toRadians(yaw), new Vector3f(0, 1, 0), viewMatrix, viewMatrix);
		Vector3f negativeCameraPos = new Vector3f(-center.x, -center.y, -center.z);
		Matrix4f.translate(negativeCameraPos, viewMatrix, viewMatrix);

		Matrix4f.mul(projectionMatrix, viewMatrix, projectionViewMatrix);
	}

}
