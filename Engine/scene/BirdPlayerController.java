package scene;

import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector3f;
import extra.Camera;
import terrain.flat.FlatTerrain;

/**
 * Kuş karakterini kontrol eden modern oyuncu kontrolcü bileşeni.
 * Yerçekimi, zıplama (uçma ivmesi), WASD yön kontrolleri, pürüzsüz dönme
 * ve durma/uçma animasyon geçişlerini kontrol eder.
 */
public class BirdPlayerController extends Component {

	private final Camera camera;
	private final FlatTerrain terrain;

	private float velocityY = 0.0f;
	private float gravity = -35.0f;     // Süzülmeyi ve düşüşü hissettiren yerçekimi ivmesi
	private float jumpForce = 18.0f;    // SPACE tuşuyla zıplama/uçma ivmesi
	private float moveSpeed = 35.0f;    // Yatay uçuş hızı

	private float hoverTime = 0.0f;
	private boolean onGround = false;
	private float modelYawOffset = 0.0f; // Blender/GLB export yaw offset (Varsayılan 0)
	private float modelPitchOffset = 0.0f; // Blender/GLB export pitch offset
	private float modelRollOffset = 0.0f; // Blender/GLB export roll offset

	public BirdPlayerController(Camera camera, FlatTerrain terrain) {
		this.camera = camera;
		this.terrain = terrain;
	}

	public void setModelYawOffset(float offset) {
		this.modelYawOffset = offset;
	}

	public float getModelYawOffset() {
		return modelYawOffset;
	}

	public void setModelPitchOffset(float offset) {
		this.modelPitchOffset = offset;
	}

	public float getModelPitchOffset() {
		return modelPitchOffset;
	}

	public void setModelRollOffset(float offset) {
		this.modelRollOffset = offset;
	}

	public float getModelRollOffset() {
		return modelRollOffset;
	}

	@Override
	public void start() {
		if (gameObject != null) {
			// Kuşun frustum dışına çıkıp yok olmasını (kaybolmalarını) önlemek için 
			// culling yarıçapını çok büyük ayarlıyoruz.
			gameObject.setCullingRadius(500.0f);
		}
	}

	@Override
	public void update(float delta) {
		if (gameObject == null) return;

		// Eğer FREE kamera modundaysak kuşu kontrol etme
		if (camera.getMode() == Camera.CameraMode.FREE) {
			scene.animation.Animator animator = gameObject.getAnimator();
			if (animator != null) {
				animator.pause();
			}
			return;
		}

		float x = gameObject.getPosition().x;
		float y = gameObject.getPosition().y;
		float z = gameObject.getPosition().z;

		// 1. Dikey ve Yatay Hareket Hesaplamaları
		float groundHeight = terrain.getHeightAt(x, z);

		if (y <= groundHeight + 0.05f) {
			onGround = true;
			y = groundHeight;
			if (velocityY < 0) {
				velocityY = 0;
			}
		} else {
			onGround = false;
		}

		boolean isMoving = false;
		float moveX = 0f;
		float moveY = 0f;
		float moveZ = 0f;

		// SHIFT tuşuna basıldığında kameranın bakış yönüne doğru uç
		if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
			float yawRad = (float) Math.toRadians(camera.getYaw());
			float pitchRad = (float) Math.toRadians(camera.getPitch());

			// 3D Yön Vektörleri (Kameranın baktığı 3D yön)
			moveX = (float) (Math.sin(yawRad) * Math.cos(pitchRad));
			moveZ = (float) (-Math.cos(yawRad) * Math.cos(pitchRad));
			moveY = (float) (-Math.sin(pitchRad));

			// Hareketi uygula
			x += moveX * moveSpeed * delta;
			z += moveZ * moveSpeed * delta;
			y += moveY * moveSpeed * delta;
			velocityY = moveY * moveSpeed; // Dikey hızı hareket hızına eşitle
			onGround = false;
			isMoving = true;
		} else {
			// SHIFT'e basılmıyorsa yerçekimi ile düşüş
			if (!onGround) {
				velocityY += gravity * delta;
				y += velocityY * delta;
			}
		}

		// Yerin altına düşmesini engelle
		if (y < groundHeight) {
			y = groundHeight;
			onGround = true;
			velocityY = 0;
		}

		// 2. Yönelim Kontrolleri (Mouse / Kamera nereye bakıyorsa kuş o yöne pürüzsüzce döner)
		float currentYaw = gameObject.getRotation().y;
		float currentPitch = gameObject.getRotation().x;

		// Yatay dönüş (Yaw)
		float targetYaw = 180 - camera.getYaw();
		float targetVisualYaw = targetYaw + modelYawOffset;
		float diffYaw = targetVisualYaw - currentYaw;
		while (diffYaw < -180) diffYaw += 360;
		while (diffYaw > 180) diffYaw -= 360;
		gameObject.getRotation().y = currentYaw + diffYaw * 10f * delta;

		// Dikey eğim (Pitch) - Uçtuğu dikey yöne göre eğilme
		float targetPitch = -camera.getPitch();
		float targetVisualPitch = targetPitch + modelPitchOffset;
		float diffPitch = targetVisualPitch - currentPitch;
		gameObject.getRotation().x = currentPitch + diffPitch * 10f * delta;

		gameObject.getPosition().set(x, y, z);
		gameObject.getRotation().z = modelRollOffset;

		// 3. Animasyon ve Durum (Idle/Uçuş) Yönetimi
		scene.animation.Animator animator = gameObject.getAnimator();
		if (animator != null) {
			if (onGround && !isMoving) {
				// Durma (Idle): Animasyonu dondur ve hafif hover / nefes alma hareketi yap
				animator.pause();
				hoverTime += delta * 3.0f;
				float hoverOffset = (float) Math.sin(hoverTime) * 0.15f;
				gameObject.getPosition().y = groundHeight + hoverOffset;
			} else {
				// Hareket Ediyor veya Uçuyor
				animator.resume();
				
				// Dikey hıza göre kanat çırpma hızını ayarla (Daha gerçekçi)
				float speedFactor = 1.0f;
				if (velocityY > 0) {
					speedFactor = 1.5f; // Yükselirken hızlı çırpınsın
				} else if (velocityY < -5f) {
					speedFactor = 0.5f; // Süzülürken yavaş çırpınsın
				}
				animator.setSpeed(speedFactor);
			}
		}
	}
}
