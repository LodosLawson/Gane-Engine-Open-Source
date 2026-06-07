package extra;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import utils.ICamera;
import utils.SmoothFloat;

/**
 * 3D dünyada oyuncunun veya kullanıcının görüş açısını (kamerasını) temsil
 * eder.
 * Kameranın hareketi, farenin döndürülmesi, yakınlaştırma/uzaklaştırma (zoom)
 * işlemleri burada hesaplanır.
 */
public class Camera implements ICamera {

	public enum CameraMode {
		FREE, FIRST_PERSON, RPG_THIRD_PERSON, ISOMETRIC, EDITOR
	}

	private CameraMode currentMode = CameraMode.EDITOR;

	/**
	 * Kameranın takip edeceği nesne (Örn: Oyuncu/Player)
	 */
	private scene.Entity target;

	/**
	 * Görüş açısı (Field of View). Kameranın ne kadar geniş bir alanı gördüğünü
	 * belirler.
	 */
	private float FOV = 60;

	public float getFOV() {
		return FOV;
	}

	public void setFOV(float fov) {
		this.FOV = fov;
		this.projectionMatrix = createProjectionMatrix();
	}

	/**
	 * Yakın kırpma düzlemi. Kameraya bu değerden daha yakın olan objeler çizilmez.
	 */
	private static final float NEAR_PLANE = 0.1f;

	/**
	 * Uzak kırpma düzlemi. Kameraya bu değerden daha uzak olan objeler çizilmez.
	 */
	private static final float FAR_PLANE = 50000;

	/**
	 * Perspektif matrisi. 3D dünyayı 2D ekrana yansıtmak için kullanılır.
	 */
	private Matrix4f projectionMatrix;

	/**
	 * Görüş matrisi. Kameranın dünyadaki pozisyonunu ve bakış açısını tutar.
	 */
	private Matrix4f viewMatrix = new Matrix4f();
	private final Matrix4f projectionViewMatrix = new Matrix4f();
	private final Vector3f pitchAxis = new Vector3f(1, 0, 0);
	private final Vector3f yawAxis = new Vector3f(0, 1, 0);
	private final Vector3f rollAxis = new Vector3f(0, 0, 1);
	private final Vector3f negativePosition = new Vector3f();

	private float fpPitchOffset = 0;
	private float fpYawOffset = 0;

	/**
	 * Kameranın X, Y, Z koordinatlarındaki güncel pozisyonu.
	 */
	private Vector3f position = new Vector3f(0, 0, 0);

	/**
	 * Kameranın yukarı/aşağı bakış açısı (eğim).
	 */
	private float pitch = 10;

	/**
	 * Kameranın sağa/sola bakış açısı (sapma).
	 */
	private float yaw = 0;

	/**
	 * Kameranın kendi ekseni etrafında dönme açısı (yuvarlanma).
	 */
	private float roll;

	/**
	 * Kameranın oyuncu (veya odak noktası) etrafındaki dönüş açısı.
	 * Yumuşak (smooth) bir geçiş sağlamak için SmoothFloat kullanılmıştır.
	 */
	private SmoothFloat angleAroundPlayer = new SmoothFloat(0, 10);

	/**
	 * Kameranın oyuncuya (veya odak noktasına) olan uzaklığı (Zoom seviyesi).
	 */
	private SmoothFloat distanceFromPlayer = new SmoothFloat(20, 5);

	/**
	 * Kameranın odaklandığı merkez noktanın X, Y (burada Z yerine kullanılıyor)
	 * konumu.
	 */
	private Vector2f center = new Vector2f();

	/**
	 * Yapıcı metot. Perspektif matrisini oluşturarak kamerayı başlatır.
	 */
	public Camera() {
		this.projectionMatrix = createProjectionMatrix();
	}

	public void setTarget(scene.Entity target) {
		this.target = target;
	}

	public scene.Entity getTarget() {
		return target;
	}

	public void setMode(CameraMode mode) {
		this.currentMode = mode;
	}

	public CameraMode getMode() {
		return currentMode;
	}

	/**
	 * Kameranın her karede (frame) hareketini günceller.
	 * Mevcut kamera moduna göre ilgili hareket fonksiyonunu çağırır.
	 */
	public void move(float delta) {
		switch (currentMode) {
			case FREE:
				moveFree(delta);
				break;
			case FIRST_PERSON:
				moveFirstPerson(delta);
				break;
			case RPG_THIRD_PERSON:
				moveRPG(delta);
				break;
			case ISOMETRIC:
				moveIsometric(delta);
				break;
			case EDITOR:
				moveEditor(delta);
				break;
		}
		updateViewMatrix();
	}

	private void moveFree(float delta) {
		if (Mouse.isButtonDown(1)) {
			pitch -= Mouse.getDY() * 0.1f;
			yaw += Mouse.getDX() * 0.1f;
		}
		if (pitch < -90)
			pitch = -90;
		if (pitch > 90)
			pitch = 90;
		yaw %= 360;

		float speed = 50f * delta;
		if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
			speed = 25f * delta;

		if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
			position.x += speed * Math.sin(Math.toRadians(yaw));
			position.z -= speed * Math.cos(Math.toRadians(yaw));
			position.y -= speed * Math.sin(Math.toRadians(pitch));
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
			position.x -= speed * Math.sin(Math.toRadians(yaw));
			position.z += speed * Math.cos(Math.toRadians(yaw));
			position.y += speed * Math.sin(Math.toRadians(pitch));
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
			position.x -= speed * Math.cos(Math.toRadians(yaw));
			position.z -= speed * Math.sin(Math.toRadians(yaw));
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
			position.x += speed * Math.cos(Math.toRadians(yaw));
			position.z += speed * Math.sin(Math.toRadians(yaw));
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_Q)) {
			position.y -= speed;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_E)) {
			position.y += speed;
		}
	}

	private void moveFirstPerson(float delta) {
		if (target == null)
			return;

		// Sadece fare hareketleriyle bakış açısı (Pitch, Yaw) değişir (kamera kafası)
		if (Mouse.isGrabbed() || Mouse.isButtonDown(0) || Mouse.isButtonDown(1)) {
			fpPitchOffset -= Mouse.getDY() * 0.1f;
			fpYawOffset += Mouse.getDX() * 0.1f;

			if (fpPitchOffset < -90)
				fpPitchOffset = -90;
			if (fpPitchOffset > 90)
				fpPitchOffset = 90;
			fpYawOffset %= 360;
		}

		// Uçağın rotasyonuna kafa rotasyonunu (offset) ekliyoruz
		this.pitch = target.getRotation().x + fpPitchOffset;
		this.yaw = target.getRotation().y + fpYawOffset;
		this.roll = target.getRotation().z; // Uçağın yatması kameraya yansır

		// FPS modunda kamera oyuncunun kokpit offset'inde durur (rotasyona göre çevrilmiş)
		Vector3f offset = target.getFirstPersonOffset();
		
		Matrix4f rotMat = new Matrix4f();
		rotMat.setIdentity();
		// Y, X, Z sırasıyla rotasyon uygula (Oyun motorunun dünya standartlarına göre)
		Matrix4f.rotate((float) Math.toRadians(target.getRotation().y), yawAxis, rotMat, rotMat);
		Matrix4f.rotate((float) Math.toRadians(target.getRotation().x), pitchAxis, rotMat, rotMat);
		Matrix4f.rotate((float) Math.toRadians(target.getRotation().z), rollAxis, rotMat, rotMat);

		org.lwjgl.util.vector.Vector4f rotatedOffset = new org.lwjgl.util.vector.Vector4f(offset.x, offset.y, offset.z, 1.0f);
		Matrix4f.transform(rotMat, rotatedOffset, rotatedOffset);

		position.x = target.getPosition().x + rotatedOffset.x;
		position.y = target.getPosition().y + rotatedOffset.y;
		position.z = target.getPosition().z + rotatedOffset.z;
	}

	private void moveRPG(float delta) {
		if (target == null)
			return;

		// RPG (Üçüncü Şahıs) kamerası oyuncuyu merkez alır
		center.x = target.getPosition().x;
		center.y = target.getPosition().z; // 2D y ekseni olarak z'yi kullanıyoruz

		calculatePitch();
		calculateAngleAroundPlayer(delta);
		calculateZoom(delta);

		float horizontalDistance = calculateHorizontalDistance();
		float verticalDistance = calculateVerticalDistance();

		float theta = angleAroundPlayer.get();
		float offsetX = (float) (horizontalDistance * Math.sin(Math.toRadians(theta)));
		float offsetZ = (float) (horizontalDistance * Math.cos(Math.toRadians(theta)));

		position.x = offsetX + center.x;
		position.z = offsetZ + center.y;
		position.y = verticalDistance + target.getPosition().y + 1.0f; // Oyuncunun biraz yukarısı

		this.yaw = 360 - angleAroundPlayer.get();
		yaw %= 360;
	}

	private void moveIsometric(float delta) {
		if (target == null)
			return;

		center.x = target.getPosition().x;
		center.y = target.getPosition().z;

		// Sabit İzometrik Açılar
		pitch = 35;
		yaw = 45;

		distanceFromPlayer.setTarget(50);
		distanceFromPlayer.update(delta);

		float horizontalDistance = calculateHorizontalDistance();
		float verticalDistance = calculateVerticalDistance();

		// İzometrikte kamera yörüngesi sabit (-45 derece)
		float theta = -45;
		float offsetX = (float) (horizontalDistance * Math.sin(Math.toRadians(theta)));
		float offsetZ = (float) (horizontalDistance * Math.cos(Math.toRadians(theta)));

		position.x = offsetX + center.x;
		position.z = offsetZ + center.y;
		position.y = verticalDistance + target.getPosition().y;
	}

	private void moveEditor(float delta) {
		// Editör modunda sadece Sağ Tık (1) basılıyken kamera hareket edebilir.
		if (Mouse.isButtonDown(1)) {
			// Bakış açısını (pitch, yaw) değiştir
			pitch -= Mouse.getDY() * 0.2f;
			yaw += Mouse.getDX() * 0.2f;
			
			if (pitch < -90) pitch = -90;
			if (pitch > 90) pitch = 90;
			yaw %= 360;

			float speed = 0.5f;

			// WASD + QE ile uzayda serbest uçuş
			if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
				position.x += speed * Math.sin(Math.toRadians(yaw));
				position.z -= speed * Math.cos(Math.toRadians(yaw));
				position.y -= speed * Math.sin(Math.toRadians(pitch));
			}
			if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
				position.x -= speed * Math.sin(Math.toRadians(yaw));
				position.z += speed * Math.cos(Math.toRadians(yaw));
				position.y += speed * Math.sin(Math.toRadians(pitch));
			}
			if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
				position.x -= speed * Math.cos(Math.toRadians(yaw));
				position.z -= speed * Math.sin(Math.toRadians(yaw));
			}
			if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
				position.x += speed * Math.cos(Math.toRadians(yaw));
				position.z += speed * Math.sin(Math.toRadians(yaw));
			}
			if (Keyboard.isKeyDown(Keyboard.KEY_Q)) {
				position.y -= speed;
			}
			if (Keyboard.isKeyDown(Keyboard.KEY_E)) {
				position.y += speed;
			}
		}
	}

	@Override
	public Vector3f getPosition() {
		return position;
	}

	public void setPitch(float pitch) {
		this.pitch = pitch;
	}

	public void setYaw(float yaw) {
		this.yaw = yaw;
	}

	@Override
	public Matrix4f getViewMatrix() {
		return viewMatrix;
	}

	/**
	 * Su gibi yansıtıcı yüzeyler için kamerayı ters çevirir (yansıma efekti).
	 * 
	 * @param height Yansıtıcı yüzeyin yüksekliği (Örn: su seviyesi).
	 */
	@Override
	public void reflect(float height) {
		this.pitch = -pitch;
		this.position.y = position.y - 2 * (position.y - height);
		updateViewMatrix();
	}

	@Override
	public Matrix4f getProjectionMatrix() {
		return projectionMatrix;
	}

	@Override
	public Matrix4f getProjectionViewMatrix() {
		// Görüş matrisi ile perspektif matrisini çarparak ortak bir matris döndürür.
		Matrix4f.mul(projectionMatrix, viewMatrix, projectionViewMatrix);
		return projectionViewMatrix;
	}

	/**
	 * Yukarı/aşağı bakış açısını tersine çevirir.
	 */
	public void invertPitch() {
		this.pitch = -pitch;
	}

	public float getPitch() {
		return pitch;
	}

	public float getYaw() {
		return yaw;
	}

	public float getRoll() {
		return roll;
	}

	/**
	 * Kameranın pozisyonu ve rotasyonlarına göre görüş (view) matrisini günceller.
	 */
	private void updateViewMatrix() {
		viewMatrix.setIdentity();
		Matrix4f.rotate((float) Math.toRadians(pitch), pitchAxis, viewMatrix, viewMatrix);
		Matrix4f.rotate((float) Math.toRadians(yaw), yawAxis, viewMatrix, viewMatrix);
		Matrix4f.rotate((float) Math.toRadians(roll), rollAxis, viewMatrix, viewMatrix);
		negativePosition.set(-position.x, -position.y, -position.z);
		Matrix4f.translate(negativePosition, viewMatrix, viewMatrix);
	}



	private Matrix4f createProjectionMatrix() {
		Matrix4f projectionMatrix = new Matrix4f();
		float aspectRatio = (float) Display.getWidth() / (float) Display.getHeight();
		float y_scale = (float) ((1f / Math.tan(Math.toRadians(FOV / 2f))));
		float x_scale = y_scale / aspectRatio;
		float frustum_length = FAR_PLANE - NEAR_PLANE;

		projectionMatrix.m00 = x_scale;
		projectionMatrix.m11 = y_scale;
		projectionMatrix.m22 = -((FAR_PLANE + NEAR_PLANE) / frustum_length);
		projectionMatrix.m23 = -1;
		projectionMatrix.m32 = -((2 * NEAR_PLANE * FAR_PLANE) / frustum_length);
		projectionMatrix.m33 = 0;
		return projectionMatrix;
	}

	/**
	 * Yatay ve dikey uzaklıklara göre kameranın dünyadaki 3 boyutlu konumunu
	 * hesaplar.
	 * 
	 * @param horizDistance  Kameranın merkeze olan yatay uzaklığı.
	 * @param verticDistance Kameranın merkeze olan dikey uzaklığı.
	 */
	private void calculateCameraPosition(float horizDistance, float verticDistance) {
		float theta = angleAroundPlayer.get();
		float offsetX = (float) (horizDistance * Math.sin(Math.toRadians(theta)));
		float offsetZ = (float) (horizDistance * Math.cos(Math.toRadians(theta)));
		position.x = offsetX + center.x;
		position.z = offsetZ + center.y;
		position.y = verticDistance + 2;
	}

	/**
	 * Klavyedeki W, A, S, D veya ok tuşlarına basıldığında kameranın odaklandığı
	 * merkez noktasını hareket ettirir.
	 */
	private void movePosition() {
		float speed = 0;
		if (Keyboard.isKeyDown(Keyboard.KEY_W) || Keyboard.isKeyDown(Keyboard.KEY_UP)) {
			speed = 0.05f;
		} else if (Keyboard.isKeyDown(Keyboard.KEY_S) || Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
			speed = -0.05f;
		}
		center.x += speed * Math.sin(Math.toRadians(yaw));
		center.y += speed * -Math.cos(Math.toRadians(yaw));
		speed = 0;
		if (Keyboard.isKeyDown(Keyboard.KEY_A) || Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
			speed = -0.05f;
		} else if (Keyboard.isKeyDown(Keyboard.KEY_D) || Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
			speed = 0.05f;
		}
		center.x += speed * Math.sin(Math.toRadians(yaw + 90));
		center.y += speed * -Math.cos(Math.toRadians(yaw + 90));
	}

	/**
	 * Kameranın merkeze olan yatay uzaklığını hesaplar (Trigonometri kullanılarak).
	 * 
	 * @return Yatay uzaklık.
	 */
	private float calculateHorizontalDistance() {
		return (float) (distanceFromPlayer.get() * Math.cos(Math.toRadians(pitch)));
	}

	/**
	 * Kameranın merkeze olan dikey (yükseklik) uzaklığını hesaplar (Trigonometri
	 * kullanılarak).
	 * 
	 * @return Dikey uzaklık.
	 */
	private float calculateVerticalDistance() {
		return (float) (distanceFromPlayer.get() * Math.sin(Math.toRadians(pitch)));
	}

	/**
	 * Farenin sağ tuşuna basılıp sürüklendiğinde kameranın yukarı/aşağı bakış
	 * açısını (pitch) hesaplar.
	 */
	private void calculatePitch() {
		if (Mouse.isButtonDown(1)) {
			float pitchChange = Mouse.getDY() * 0.2f;
			pitch -= pitchChange;
			if (pitch < -90f) {
				pitch = -90f;
			} else if (pitch > 90) {
				pitch = 90;
			}
		}
	}

	/**
	 * Fare tekerleği (scroll) çevrildiğinde kameranın yakınlaştırma (zoom)
	 * seviyesini hesaplar.
	 */
	private void calculateZoom(float delta) {
		float targetZoom = distanceFromPlayer.getTarget();
		float zoomLevel = Mouse.getDWheel() * 0.0008f * targetZoom;
		targetZoom -= zoomLevel;
		if (targetZoom < 1) {
			targetZoom = 1;
		} else if (targetZoom > 500) {
			targetZoom = 500;
		}
		distanceFromPlayer.setTarget(targetZoom);
		distanceFromPlayer.update(delta);
	}

	/**
	 * Farenin sol tuşuna basılıp sürüklendiğinde kameranın merkez etrafında dönüş
	 * açısını hesaplar.
	 */
	private void calculateAngleAroundPlayer(float delta) {
		if (Mouse.isButtonDown(0) || Mouse.isButtonDown(1)) {
			float angleChange = Mouse.getDX() * 0.3f;
			angleAroundPlayer.increaseTarget(-angleChange);
		} else if (Keyboard.isKeyDown(Keyboard.KEY_R)) {
			angleAroundPlayer.increaseTarget(100.0f * delta);
		}
		angleAroundPlayer.update(delta);
	}
}
