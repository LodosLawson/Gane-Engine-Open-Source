package default_controls;

import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import scene.Component;
import scene.GameObject;

public class PlaneController extends Component {

	private extra.Camera camera;
	private terrain.flat.FlatTerrain terrain;

	private boolean isActive = false;

	// PHYSICS
	private Vector3f velocity = new Vector3f(0, 0, 0);
	private float currentThrottle = 0.0f; // 0.0 (Idle) - 1.0 (Max)
	private float maxThrust = 15000.0f;
	private float mass = 1200.0f;
	private float gravity = 9.81f;
	private float dragCoeff = 0.02f;
	private float liftCoeff = 0.08f;

	// EULER SPEEDS (deg/sec)
	private float pitchSpeed = 45.0f;
	private float yawSpeed = 25.0f;
	private float rollSpeed = 60.0f;

	private java.util.List<scene.GameObject> propellers = new java.util.ArrayList<>();

	// Kalibrasyon Ofsetleri (Modelin kendi eksen hatasını düzeltmek için)
	// Normalde OpenGL'de -Z ileridir, ancak uçak modelleri farklı kaydedilmiş
	// olabilir.
	private float modelOffsetX = -90.0f; // Varsayılan GLTF düzeltmesi
	private float modelOffsetY = 0.0f;
	private float modelOffsetZ = -90.0f;

	public PlaneController(extra.Camera camera, terrain.flat.FlatTerrain terrain) {
		this.camera = camera;
		this.terrain = terrain;
	}

	public void setActive(boolean active) {
		this.isActive = active;
	}

	@Override
	public void start() {
		// Pervaneleri bul
		if (gameObject != null && gameObject.getMultiMeshParts() != null) {
			for (scene.GameObject part : gameObject.getMultiMeshParts()) {
				if (part.getModel() != null && part.getModel().getModelData() != null) {
					String name = part.getModel().getModelData().getName();
					if (name != null) {
						String lower = name.toLowerCase();
						System.out.println("[PlaneController] Bulunan Parca: " + name);
						// Pervane adını bilmediğimiz için bütün parçaları listeye ekliyoruz.
						// Oyun içinde 'M' tuşuna basarak hangisinin pervane olduğunu bulacağız.
						propellers.add(part);
					}
				}
			}
		}

		// Başlangıçta görsel rotasyonu uygula
		updateVisual();
	}

	@Override
	public void update(float delta) {
		if (!isActive)
			return;

		// 1. INPUT
		// KALİBRASYON: Tüm eksenlerde (X, Y, Z) uçağın modelini çevirip düzeltmek için
		if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_I)) {
			modelOffsetX += 45.0f * delta;
			updateVisual();
		} else if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_K)) {
			modelOffsetX -= 45.0f * delta;
			updateVisual();
		}

		if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_J)) {
			modelOffsetY += 45.0f * delta;
			updateVisual();
		} else if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_L)) {
			modelOffsetY -= 45.0f * delta;
			updateVisual();
		}

		if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_U)) {
			modelOffsetZ += 45.0f * delta;
			updateVisual();
		} else if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_O)) {
			modelOffsetZ -= 45.0f * delta;
			updateVisual();
		}

		if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT)
				|| org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT)) {
			currentThrottle = Math.min(1.0f, currentThrottle + delta * 0.5f);
		} else if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
			currentThrottle = Math.max(0.0f, currentThrottle - delta * 0.5f);
		}

		float currentSpeed = velocity.length();
		float controlFactor = Math.min(1.0f, Math.max(0.1f, currentSpeed / 40.0f));

		// Pitch (W/S)
		if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
			gameObject.getRotation().x -= pitchSpeed * controlFactor * delta; // Nose up
		} else if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
			gameObject.getRotation().x += pitchSpeed * controlFactor * delta; // Nose down
		}

		// Roll (A/D) - GERÇEKÇİ DÖNÜŞ İÇİN YANA YATMA
		float rollSpeed = 60.0f;
		if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
			gameObject.getRotation().z -= rollSpeed * controlFactor * delta;
		} else if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
			gameObject.getRotation().z += rollSpeed * controlFactor * delta;
		}

		// Yaw (Q/E) - RUDDER / KUYRUK DÖNÜŞÜ
		float yawSpeed = 30.0f;
		if (Keyboard.isKeyDown(Keyboard.KEY_Q)) {
			gameObject.getRotation().y += yawSpeed * controlFactor * delta;
		} else if (Keyboard.isKeyDown(Keyboard.KEY_E)) {
			gameObject.getRotation().y -= yawSpeed * controlFactor * delta;
		}

		// 2. KUVVET VE VEKTÖR HESAPLAMALARI
		float yaw = (float) Math.toRadians(gameObject.getRotation().y);
		float pitch = (float) Math.toRadians(gameObject.getRotation().x);
		float roll = (float) Math.toRadians(gameObject.getRotation().z);

		// Rotasyon matrisi oluştur (EntityRenderer ile aynı sıra: Y -> X -> Z)
		Matrix4f rotMat = new Matrix4f();
		rotMat.setIdentity();
		Matrix4f.rotate(yaw, new Vector3f(0, 1, 0), rotMat, rotMat);
		Matrix4f.rotate(pitch, new Vector3f(1, 0, 0), rotMat, rotMat);
		Matrix4f.rotate(roll, new Vector3f(0, 0, 1), rotMat, rotMat);

		// Local vektörler (OpenGL default: -Z ileri, +Y yukarı, +X sağ)
		Vector4f forward4 = Matrix4f.transform(rotMat, new Vector4f(0, 0, -1, 0), null);
		Vector4f up4 = Matrix4f.transform(rotMat, new Vector4f(0, 1, 0, 0), null);
		Vector4f right4 = Matrix4f.transform(rotMat, new Vector4f(1, 0, 0, 0), null);

		Vector3f forward = new Vector3f(forward4.x, forward4.y, forward4.z);
		Vector3f up = new Vector3f(up4.x, up4.y, up4.z);
		Vector3f right = new Vector3f(right4.x, right4.y, right4.z);

		if (forward.lengthSquared() > 0)
			forward.normalise();
		if (up.lengthSquared() > 0)
			up.normalise();
		if (right.lengthSquared() > 0)
			right.normalise();

		float speedSq = velocity.lengthSquared();
		float speed = (float) Math.sqrt(speedSq);

		Vector3f velDir = new Vector3f(0, 0, 0);
		if (speed > 0.1f) {
			velDir.set(velocity.x / speed, velocity.y / speed, velocity.z / speed);
		} else {
			velDir.set(forward);
		}

		// GERÇEKÇİ FİZİK: Angle of Attack (Hücum Açısı) Hesaplama
		float v_fwd = Vector3f.dot(velocity, forward);
		float v_up = Vector3f.dot(velocity, up);

		float alpha = 0.0f;
		if (speed > 0.1f) {
			alpha = (float) Math.atan2(-v_up, Math.max(0.1f, v_fwd));
		}
		float alphaDeg = (float) Math.toDegrees(alpha);

		// Aerodinamik Katsayılar
		float Cl = 0.0f;
		float Cd = 0.0f;
		float stallAngle = 15.0f;

		// Lift Eğrisi (Lift Curve)
		if (Math.abs(alphaDeg) < stallAngle) {
			Cl = alphaDeg * 0.1f; // Lineer artış
		} else {
			// Stall (Tutunma Kaybı): Açı büyüdükçe lift düşer
			Cl = Math.signum(alphaDeg) * 1.5f * (float) Math.exp(-Math.abs(alphaDeg - stallAngle) * 0.1f);
		}

		// Drag Eğrisi
		float Cd0 = 0.05f; // Temel sürtünme
		float inducedDrag = 0.05f * Cl * Cl; // Kaldırma kuvvetinden doğan sürtünme
		float stallDrag = 0.0f;
		if (Math.abs(alphaDeg) >= stallAngle) {
			stallDrag = Math.abs(alphaDeg) * 0.02f; // Stall durumunda aşırı sürtünme
		}
		Cd = Cd0 + inducedDrag + stallDrag;

		float aeroFactor = 25.0f; // DAHA KOLAY UÇUŞ: Lift Çarpanı artırıldı (Eski 15.0)

		// Drag Force (Sürtünme): Hareket yönüne zıt
		float dragMag = aeroFactor * speedSq * Cd;
		Vector3f dragForce = new Vector3f(-velDir.x * dragMag, -velDir.y * dragMag, -velDir.z * dragMag);

		// Lift Force (Kaldırma): Hız vektörü ve Sağ kanat vektörüne dik
		Vector3f liftDir = new Vector3f();
		Vector3f.cross(right, velDir, liftDir);
		if (liftDir.lengthSquared() > 0) {
			liftDir.normalise();
		} else {
			liftDir.set(up);
		}

		float liftMag = aeroFactor * speedSq * Cl;
		Vector3f liftForce = new Vector3f(liftDir.x * liftMag, liftDir.y * liftMag, liftDir.z * liftMag);

		// Thrust (İtki)
		float engineMaxThrust = 12000.0f; // DAHA GÜÇLÜ MOTOR (Eski 8000.0)
		Vector3f thrustForce = new Vector3f(forward.x * currentThrottle * engineMaxThrust,
				forward.y * currentThrottle * engineMaxThrust,
				forward.z * currentThrottle * engineMaxThrust);

		// Gravity (Yerçekimi)
		float planeMass = 1000.0f; // DAHA HAFİF UÇAK (Eski 1200.0)
		Vector3f gravityForce = new Vector3f(0, -gravity * planeMass, 0);

		// Toplam Kuvvet
		Vector3f totalForce = new Vector3f(0, 0, 0);
		Vector3f.add(totalForce, thrustForce, totalForce);
		Vector3f.add(totalForce, gravityForce, totalForce);
		Vector3f.add(totalForce, dragForce, totalForce);
		Vector3f.add(totalForce, liftForce, totalForce);

		// İvme (Acceleration = F / m)
		Vector3f acceleration = new Vector3f(totalForce.x / planeMass, totalForce.y / planeMass,
				totalForce.z / planeMass);

		// Hızı güncelle (v = v0 + a*t)
		velocity.x += acceleration.x * delta;
		velocity.y += acceleration.y * delta;
		velocity.z += acceleration.z * delta;

		// --- UÇAĞIN BAKTIĞI YÖNE GİTMESİNİ SAĞLAMA (KAPATILDI) ---
		// Gerçekçi uçuş istendiği için, uçak artık Roll yapınca Lift vektörü sayesinde dönecek.
		// Drift'i tamamen kapatan arcade stabilizasyon kodunu sildik.
		
		// Eğer uçak çok düşük hızlarda yana kayıyorsa diye çok ufak bir yatay sürtünme:
		float currentRealSpeed = velocity.length();
		if (currentRealSpeed > 5.0f) {
			Vector3f rightDir = new Vector3f(right);
			float lateralSpeed = Vector3f.dot(velocity, rightDir);
			// Yanlamasına olan hızımızı azaltalım ki uçak havada drift yapmasın, ama lift ile dönsün
			velocity.x -= rightDir.x * lateralSpeed * delta * 2.0f;
			velocity.y -= rightDir.y * lateralSpeed * delta * 2.0f;
			velocity.z -= rightDir.z * lateralSpeed * delta * 2.0f;
		}

		// Zemin kontrolü
		float terrainHeight = terrain != null
				? terrain.getHeightAt(gameObject.getPosition().x, gameObject.getPosition().z)
				: 0;
		if (gameObject.getPosition().y <= terrainHeight + 1.0f) {
			gameObject.getPosition().y = terrainHeight + 1.0f;

			// Yere çarpma
			if (velocity.y < 0) {
				velocity.y = 0;
			}

			// Yerdeyken tekerlek sürtünmesi (Yuvarlanma Direnci)
			// Hıza bağımlı hafif bir yavaşlama (Saniyede %5 kayıp - Daha az sürtünme)
			velocity.x -= velocity.x * delta * 0.05f;
			velocity.z -= velocity.z * delta * 0.05f;
		}

		// Pozisyonu güncelle
		gameObject.getPosition().x += velocity.x * delta;
		gameObject.getPosition().y += velocity.y * delta;
		gameObject.getPosition().z += velocity.z * delta;

		// Pervane Animasyonu ve Yeri Ayarlama (Numpad 8, 2, 4, 6, 7, 9)
		if (gameObject.getMultiMeshParts() != null) {
			for (scene.GameObject part : gameObject.getMultiMeshParts()) {
				if (part.getModel() != null && part.getModel().getModelData() != null) {
					String partName = part.getModel().getModelData().getName();
					if (partName != null && partName.equalsIgnoreCase("Pervane")) {
						// Pervanenin yerel rotasyonunu Z ekseninde döndür
						if (currentThrottle > 0.01f) {
							part.getModelOffsetRot().z -= currentThrottle * 4000.0f * delta;
						}
						
						// --- GEÇİCİ PERVANE KONUM AYARLAYICI ---
						// Pervane merkeze doğduğu için oyun içindeyken onu uçağın burnuna taşıyabilmen için eklendi.
						if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_NUMPAD8)) part.getBaseOffset().z += 5.0f * delta; // İleri
						if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_NUMPAD2)) part.getBaseOffset().z -= 5.0f * delta; // Geri
						if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_NUMPAD6)) part.getBaseOffset().y += 5.0f * delta; // Yukarı
						if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_NUMPAD4)) part.getBaseOffset().y -= 5.0f * delta; // Aşağı
						if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_NUMPAD9)) part.getBaseOffset().x += 5.0f * delta; // Sağa
						if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_NUMPAD7)) part.getBaseOffset().x -= 5.0f * delta; // Sola
						
						// Konumu ekrana yazdır (konsola) tuşa basıldığında
						if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_NUMPAD5)) {
							System.out.println("PERVANE OFFSET -> X: " + part.getBaseOffset().x + " Y: " + part.getBaseOffset().y + " Z: " + part.getBaseOffset().z);
						}
					}
				}
			}
		}

		// Kamera Takibi
		if (camera != null && camera.getTarget() != gameObject) {
			camera.setTarget(gameObject);
		}
	}

	private void updateVisual() {
		if (gameObject != null) {
			gameObject.getModelOffsetRot().set(modelOffsetX, modelOffsetY, modelOffsetZ);
			if (gameObject.getMultiMeshParts() != null) {
				for (scene.GameObject part : gameObject.getMultiMeshParts()) {
					part.getModelOffsetRot().set(modelOffsetX, modelOffsetY, modelOffsetZ);
				}
			}
			System.out.println("[PlaneController] Gorsel Offset (X, Y, Z): " + modelOffsetX + ", " + modelOffsetY + ", "
					+ modelOffsetZ);
		}
	}
}
