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

	public BirdPlayerController(Camera camera, FlatTerrain terrain) {
		this.camera = camera;
		this.terrain = terrain;
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

		// 1. Dikey Hareket ve Yerçekimi Hesaplamaları
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

		// SPACE tuşuna basıldığında kanat çırparak yukarı ivmelen
		if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
			velocityY = jumpForce;
			onGround = false;
		}

		// Havadaysa yerçekimini uygula
		if (!onGround) {
			velocityY += gravity * delta;
		}

		y += velocityY * delta;

		// Yerin altına düşmesini engelle
		if (y < groundHeight) {
			y = groundHeight;
			onGround = true;
			velocityY = 0;
		}

		// 2. Yatay Hareket (W, A, S, D) - Kameranın bakış açısına göre
		float currentYaw = gameObject.getRotation().y;
		float targetYaw = currentYaw;
		boolean isMovingHorizontally = false;
		float moveX = 0f;
		float moveZ = 0f;

		float yawRad = (float) Math.toRadians(camera.getYaw());

		if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
			moveX += Math.sin(yawRad);
			moveZ -= Math.cos(yawRad);
			targetYaw = 180 - camera.getYaw();
			isMovingHorizontally = true;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
			moveX -= Math.sin(yawRad);
			moveZ += Math.cos(yawRad);
			targetYaw = -camera.getYaw();
			isMovingHorizontally = true;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
			moveX -= Math.cos(yawRad);
			moveZ -= Math.sin(yawRad);
			targetYaw = 90 - camera.getYaw();
			isMovingHorizontally = true;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
			moveX += Math.cos(yawRad);
			moveZ += Math.sin(yawRad);
			targetYaw = 270 - camera.getYaw();
			isMovingHorizontally = true;
		}

		if (isMovingHorizontally) {
			// Hareketi normalleştir ki çapraz giderken hızlı gitmesin
			float len = (float) Math.sqrt(moveX * moveX + moveZ * moveZ);
			if (len > 0) {
				moveX = (moveX / len) * moveSpeed * delta;
				moveZ = (moveZ / len) * moveSpeed * delta;
			}
			x += moveX;
			z += moveZ;

			// Pürüzsüz Yönelme (Smooth Rotation)
			float diff = targetYaw - currentYaw;
			while (diff < -180) diff += 360;
			while (diff > 180) diff -= 360;
			gameObject.getRotation().y = currentYaw + diff * 10f * delta;
		}

		gameObject.getPosition().set(x, y, z);

		// 3. Animasyon ve Durum (Idle/Uçuş) Yönetimi
		scene.animation.Animator animator = gameObject.getAnimator();
		if (animator != null) {
			if (onGround && !isMovingHorizontally) {
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
