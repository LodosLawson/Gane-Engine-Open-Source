package utils;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import extra.Camera;

/**
 * 2D Ekranda fare ile tıklanılan noktanın 3D dünyada (uzayda) nereye denk geldiğini
 * bulmak için "RayCasting" (Işın gönderme) işlemi yapan yardımcı sınıf.
 */
public class MousePicker {

	private Vector3f currentRay = new Vector3f();

	private Matrix4f projectionMatrix;
	private Matrix4f viewMatrix;
	private Camera camera;

	public MousePicker(Camera cam, Matrix4f projection) {
		this.camera = cam;
		this.projectionMatrix = projection;
		this.viewMatrix = cam.getViewMatrix();
	}

	/** @return Farenin ekran konumundan 3D dünyaya doğru fırlatılan vektör (ışın) */
	public Vector3f getCurrentRay() {
		return currentRay;
	}

	/** Her karede (frame) farenin konumuna göre ışını hesaplar. */
	public void update() {
		viewMatrix = camera.getViewMatrix();
		currentRay = calculateMouseRay();
	}

	private Vector3f calculateMouseRay() {
		float mouseX = Mouse.getX();
		float mouseY = Mouse.getY(); // LWJGL'de alt sol (0,0) dır.
		Vector2f normalizedCoords = getNormalisedDeviceCoordinates(mouseX, mouseY);
		Vector4f clipCoords = new Vector4f(normalizedCoords.x, normalizedCoords.y, -1.0f, 1.0f);
		Vector4f eyeCoords = toEyeCoords(clipCoords);
		Vector3f worldRay = toWorldCoords(eyeCoords);
		return worldRay;
	}

	private Vector3f toWorldCoords(Vector4f eyeCoords) {
		Matrix4f invertedView = Matrix4f.invert(viewMatrix, null);
		Vector4f rayWorld = Matrix4f.transform(invertedView, eyeCoords, null);
		Vector3f mouseRay = new Vector3f(rayWorld.x, rayWorld.y, rayWorld.z);
		mouseRay.normalise();
		return mouseRay;
	}

	private Vector4f toEyeCoords(Vector4f clipCoords) {
		Matrix4f invertedProjection = Matrix4f.invert(projectionMatrix, null);
		Vector4f eyeCoords = Matrix4f.transform(invertedProjection, clipCoords, null);
		return new Vector4f(eyeCoords.x, eyeCoords.y, -1f, 0f);
	}

	private Vector2f getNormalisedDeviceCoordinates(float mouseX, float mouseY) {
		float x = (2.0f * mouseX) / Display.getWidth() - 1f;
		float y = (2.0f * mouseY) / Display.getHeight() - 1f;
		return new Vector2f(x, y);
	}

	/**
	 * Matematiksel olarak bir ışının belirli bir noktadan (veya küreden) geçip geçmediğini test eder.
	 * 
	 * @param rayOrigin Işının çıkış noktası (Kameranın pozisyonu)
	 * @param rayDir Işının yönü (getCurrentRay)
	 * @param target Merkez noktası (Objenin pozisyonu)
	 * @param radius Etki alanı yarıçapı
	 * @return Işın cisme değiyorsa true döner
	 */
	public static boolean intersects(Vector3f rayOrigin, Vector3f rayDir, Vector3f target, float radius) {
		Vector3f oc = Vector3f.sub(rayOrigin, target, null);
		float b = Vector3f.dot(oc, rayDir);
		float c = Vector3f.dot(oc, oc) - radius * radius;
		// Diskriminant
		float d = b * b - c;
		return d > 0;
	}
}
