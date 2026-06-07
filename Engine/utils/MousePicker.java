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
 * bulmak için "RayCasting" (Işın fırlatma) işlemi yapan yardımcı sınıf.
 * Ekrana (2D) tıklanan fare koordinatlarını alır, bunu ters çevirerek 3D uzayda kameradan 
 * ileriye doğru fırlatılan bir vektör (ışın) haline getirir.
 * 
 * <p><b>Kullanım Örneği:</b></p>
 * <pre>
 * {@code
 * MousePicker picker = new MousePicker(camera, renderer.getProjectionMatrix());
 * 
 * // Her karede farenin konumuna göre ışını güncelle
 * picker.update();
 * 
 * // Fırlatılan ışın (Ray)
 * Vector3f ray = picker.getCurrentRay();
 * 
 * // Bu ışın objeye çarpıyor mu test et
 * boolean hit = MousePicker.intersects(camera.getPosition(), ray, entity.getPosition(), 2.0f);
 * if(hit && Mouse.isButtonDown(0)) {
 *     System.out.println("Objeye tıklandı!");
 * }
 * }
 * </pre>
 */
public class MousePicker {

	/** Farenin ekran konumundan 3D dünyaya doğru fırlatılan yön vektörü (Işın) */
	private Vector3f currentRay = new Vector3f();

	/** 3D dünyayı 2D ekrana yansıtan matris */
	private Matrix4f projectionMatrix;
	/** Kameranın dünyadaki konum ve açısını tutan matris */
	private Matrix4f viewMatrix;
	/** Referans alınan kamera nesnesi */
	private Camera camera;

	/**
	 * Fare seçici (MousePicker) nesnesini başlatır.
	 * 
	 * @param cam Sahnede kullanılan aktif kamera
	 * @param projection Ekranın projeksiyon (perspektif) matrisi
	 */
	public MousePicker(Camera cam, Matrix4f projection) {
		this.camera = cam;
		this.projectionMatrix = projection;
		this.viewMatrix = cam.getViewMatrix();
	}

	/**
	 * Farenin ekran konumundan 3D dünyaya doğru fırlatılan normalize edilmiş vektörü (ışını) döndürür.
	 * 
	 * @return 3D Uzaydaki ışın yönü.
	 */
	public Vector3f getCurrentRay() {
		return currentRay;
	}

	/** 
	 * Her karede (frame) farenin o anki ekran konumuna göre yeni bir ışın (ray) hesaplar.
	 * Oyun döngüsünde (Game Loop) düzenli olarak çağrılmalıdır.
	 */
	public void update() {
		viewMatrix = camera.getViewMatrix();
		currentRay = calculateMouseRay();
	}

	/**
	 * Farenin 2D X ve Y koordinatlarını alarak bunu 3D uzayda bir vektöre dönüştürür.
	 * 
	 * @return Kameradan farenin gösterdiği yere doğru olan 3D yön vektörü.
	 */
	private Vector3f calculateMouseRay() {
		float mouseX = Mouse.getX();
		float mouseY = Mouse.getY(); // LWJGL'de y ekseni alt solda (0,0) olarak başlar.
		Vector2f normalizedCoords = getNormalisedDeviceCoordinates(mouseX, mouseY);
		// Z=-1 diyerek ışının kameradan ekrana doğru fırlatılmasını (Near Plane) sağlarız.
		Vector4f clipCoords = new Vector4f(normalizedCoords.x, normalizedCoords.y, -1.0f, 1.0f);
		Vector4f eyeCoords = toEyeCoords(clipCoords);
		Vector3f worldRay = toWorldCoords(eyeCoords);
		return worldRay;
	}

	/**
	 * Kamera/Göz koordinat sisteminden (Eye Space) Dünya koordinatlarına (World Space) geçiş yapar.
	 */
	private Vector3f toWorldCoords(Vector4f eyeCoords) {
		Matrix4f invertedView = Matrix4f.invert(viewMatrix, null);
		Vector4f rayWorld = Matrix4f.transform(invertedView, eyeCoords, null);
		Vector3f mouseRay = new Vector3f(rayWorld.x, rayWorld.y, rayWorld.z);
		mouseRay.normalise();
		return mouseRay;
	}

	/**
	 * Klip koordinatlarından (Clip Space) Kamera/Göz koordinatlarına (Eye Space) geçiş yapar.
	 */
	private Vector4f toEyeCoords(Vector4f clipCoords) {
		Matrix4f invertedProjection = Matrix4f.invert(projectionMatrix, null);
		Vector4f eyeCoords = Matrix4f.transform(invertedProjection, clipCoords, null);
		return new Vector4f(eyeCoords.x, eyeCoords.y, -1f, 0f); // Z=-1 (İleri doğru), W=0 (Yön vektörü olduğu için)
	}

	/**
	 * Farenin ekran koordinatlarını OpenGL'in anlayacağı Normalize Edilmiş Cihaz Koordinatlarına (-1 ile 1 arası) çevirir.
	 */
	private Vector2f getNormalisedDeviceCoordinates(float mouseX, float mouseY) {
		float x = (2.0f * mouseX) / Display.getWidth() - 1f;
		float y = (2.0f * mouseY) / Display.getHeight() - 1f;
		return new Vector2f(x, y);
	}

	/**
	 * Matematiksel olarak bir ışının belirli bir noktadan (veya o noktadaki hayali bir küreden) 
	 * geçip geçmediğini (Intersection) test eder.
	 * 
	 * @param rayOrigin Işının çıkış noktası (Kameranın dünya pozisyonu)
	 * @param rayDir Işının yönü (getCurrentRay() ile alınan vektör)
	 * @param target Merkez noktası (Çarpışma testi yapılacak objenin dünya pozisyonu)
	 * @param radius Etki alanı yarıçapı (Tıklamanın geçerli olacağı büyüklük)
	 * @return Işın cisme değiyorsa true, aksi halde false döner.
	 */
	public static boolean intersects(Vector3f rayOrigin, Vector3f rayDir, Vector3f target, float radius) {
		Vector3f oc = Vector3f.sub(rayOrigin, target, null);
		float b = Vector3f.dot(oc, rayDir);
		float c = Vector3f.dot(oc, oc) - radius * radius;
		
		// Diskriminant hesaplaması (B^2 - 4AC formülünün basitleştirilmiş hali)
		float d = b * b - c;
		
		// Eğer diskriminant pozitifse, ışın küreyi kesiyordur.
		return d > 0;
	}
}
