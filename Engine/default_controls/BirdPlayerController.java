package default_controls;

import scene.Component;
import scene.GameObject;
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
	private float gravity = -35.0f; // Serbest düşüş yerçekimi
	private float glideGravity = -5.0f; // Süzülme yerçekimi
	private float jumpForce = 18.0f;
	private float moveSpeed = 35.0f; // Yatay uçuş hızı

	// Süzülme (Gliding) için mevcut hızlar
	private float currentGlideSpeedX = 0f;
	private float currentGlideSpeedZ = 0f;

	private float hoverTime = 0.0f;
	private boolean onGround = false;
	private float modelYawOffset = 0.0f; // Blender/GLB export yaw offset (Varsayılan 0)
	private float modelPitchOffset = 0.0f; // Blender/GLB export pitch offset
	private float modelRollOffset = 0.0f; // Blender/GLB export roll offset
	private float currentTargetYaw = 0.0f;
	private float currentTargetPitch = 0.0f;
	private float currentRoll = 0.0f;

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
		if (gameObject == null)
			return;

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
		float minSafeHeight = Math.max(groundHeight, 6.0f); // Suyun altına girmemesi için min Y = 6.0f (Okyanus 5.0)

		if (y <= minSafeHeight + 0.05f) {
			onGround = true;
			y = minSafeHeight;
			if (velocityY < 0) {
				velocityY = 0;
			}
			currentGlideSpeedX = 0;
			currentGlideSpeedZ = 0;
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

			moveX = (float) (Math.sin(yawRad) * Math.cos(pitchRad));
			moveZ = (float) (-Math.cos(yawRad) * Math.cos(pitchRad));
			moveY = (float) (-Math.sin(pitchRad));

			// Hareketi uygula
			x += moveX * moveSpeed * delta;
			z += moveZ * moveSpeed * delta;
			y += moveY * moveSpeed * delta;
			velocityY = moveY * moveSpeed; // Dikey hızı hareket hızına eşitle

			// Süzülme için hızı kaydet
			currentGlideSpeedX = moveX * moveSpeed;
			currentGlideSpeedZ = moveZ * moveSpeed;

			onGround = false;
			isMoving = true;
		} else {
			// SHIFT'e basılmıyorsa...
			if (!onGround) {
				// Dalış (Space tuşu)
				if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
					velocityY -= 80.0f * delta; // Çok hızlı aşağı dalış
					if (velocityY < -60.0f)
						velocityY = -60.0f; // Limit dive speed
					currentTargetPitch = 70.0f; // Burnu aşağı bakacak şekilde dal
					isMoving = true; // Rotasyonu güncellemesine izin ver

					// Dalış yaparken mevcut yatay momentumu koru ama yavaşça azalt
					currentGlideSpeedX *= (1.0f - 0.5f * delta);
					currentGlideSpeedZ *= (1.0f - 0.5f * delta);
					x += currentGlideSpeedX * delta;
					z += currentGlideSpeedZ * delta;
				} else {
					// Süzülme (Glide): Yavaş düşüş ve ileri süzülme
					float maxGlideDropSpeed = -8.0f;
					velocityY += glideGravity * delta;
					if (velocityY < maxGlideDropSpeed) {
						// Çok hızlı düşüyorsa yavaşlat (örneğin dalıştan sonra süzülmeye geçiş)
						velocityY += (maxGlideDropSpeed - velocityY) * 3.0f * delta;
					}

					currentTargetPitch = 15.0f; // Süzülürken burnu hafif aşağı
					isMoving = true;

					// Süzülürken kameranın baktığı yöne doğru (yaw) belli bir hızda ilerle
					float targetGlideSpeed = 22.0f;
					float radYaw = (float) Math.toRadians(camera.getYaw());
					float targetGlideX = (float) (Math.sin(radYaw)) * targetGlideSpeed;
					float targetGlideZ = (float) (-Math.cos(radYaw)) * targetGlideSpeed;

					currentGlideSpeedX += (targetGlideX - currentGlideSpeedX) * 2.0f * delta;
					currentGlideSpeedZ += (targetGlideZ - currentGlideSpeedZ) * 2.0f * delta;

					x += currentGlideSpeedX * delta;
					z += currentGlideSpeedZ * delta;
				}
				y += velocityY * delta;
			}
		}

		// Yerin / Suyun altına düşmesini engelle
		if (y < minSafeHeight) {
			y = minSafeHeight;
			onGround = true;
			velocityY = 0;
			currentGlideSpeedX = 0;
			currentGlideSpeedZ = 0;
		}

		// 2. Yönelim Kontrolleri
		if (isMoving) {
			// Sadece hareket ederken farenin/kameranın baktığı yöne dön
			currentTargetYaw = -camera.getYaw();
			// Eğer uçuyorsak kameranın pitch'ini al, yoksa dalış/süzülme pitch'ini koru
			if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
				currentTargetPitch = camera.getPitch();
			}
		}

		float currentYaw = gameObject.getRotation().y;
		float currentPitch = gameObject.getRotation().x;

		// Dönüş yumuşaklık hızı (Daha gerçekçi, biraz daha ağır bir hissiyat için 8.0f)
		float rotationSmoothness = 8.0f;

		// Yatay dönüş (Yaw)
		float targetVisualYaw = currentTargetYaw + modelYawOffset;
		float diffYaw = targetVisualYaw - currentYaw;
		while (diffYaw < -180)
			diffYaw += 360;
		while (diffYaw > 180)
			diffYaw -= 360;
		gameObject.getRotation().y = currentYaw + diffYaw * rotationSmoothness * delta;
		// Açıların çok büyümesini engelle (Normalize)
		gameObject.getRotation().y %= 360;

		// Dikey eğim (Pitch)
		float targetVisualPitch = currentTargetPitch + modelPitchOffset;
		float diffPitch = targetVisualPitch - currentPitch;
		while (diffPitch < -180)
			diffPitch += 360;
		while (diffPitch > 180)
			diffPitch -= 360;
		gameObject.getRotation().x = currentPitch + diffPitch * rotationSmoothness * delta;
		gameObject.getRotation().x %= 360;

		// Gerçekçi Otomatik Yan Yatma (Auto-Banking) ve Manuel Roll (Q, E)
		float rollSpeed = 120.0f;
		float maxRoll = 65.0f;

		// Otomatik yatma: Kuş sağa/sola dönerken (diffYaw) doğal olarak o yöne yatar
		float targetAutoRoll = diffYaw * 1.2f;
		float targetAutoPich = diffPitch * 1.2f;

		boolean manualRoll = false;
		if (Keyboard.isKeyDown(Keyboard.KEY_Q)) {
			currentRoll -= rollSpeed * delta;
			manualRoll = true;
		} else if (Keyboard.isKeyDown(Keyboard.KEY_E)) {
			currentRoll += rollSpeed * delta;
			manualRoll = true;
		}

		if (!manualRoll) {
			// Manuel tuşlara basılmıyorsa, dönüş hızına bağlı olarak dinamik otomatik yatma
			// yap
			currentRoll += (targetAutoRoll - currentRoll) * 4.0f * delta;
			currentPitch += (targetAutoPich - currentPitch) * 4.0f * delta;
		}

		// Sınırlandırma
		if (currentRoll < -maxRoll)
			currentRoll = -maxRoll;
		if (currentRoll > maxRoll)
			currentRoll = maxRoll;

		gameObject.getPosition().set(x, y, z);
		gameObject.getRotation().z = modelRollOffset + currentRoll;

		// 3. Animasyon ve Durum (Idle/Uçuş) Yönetimi
		scene.animation.Animator animator = gameObject.getAnimator();
		if (animator != null) {
			if (onGround && !isMoving) {
				// Durma (Idle): Animasyonu dondur ve hafif hover / nefes alma hareketi yap
				animator.pause();
				hoverTime += delta * 3.0f;
				float hoverOffset = (float) Math.sin(hoverTime) * 0.15f;
				gameObject.getPosition().y = minSafeHeight + hoverOffset;
			} else {
				// Hareket Ediyor veya Uçuyor veya Süzülüyor
				animator.resume();

				// Dikey hıza göre kanat çırpma hızını ayarla (Daha gerçekçi)
				float speedFactor = 1.0f;
				if (velocityY > 5f) {
					speedFactor = 1.8f; // Yükselirken hızlı çırpınsın
				} else if (velocityY < -20f) {
					speedFactor = 0.2f; // Dalış yaparken kanat çırpmayı durdurmaya yakın (süzülüş pozu)
				} else if (velocityY < -2f) {
					speedFactor = 0.5f; // Normal süzülürken yavaş çırpınsın
				}
				animator.setSpeed(speedFactor);
			}
		}
	}
}
