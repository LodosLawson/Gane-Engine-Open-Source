package default_controls;

import scene.Component;
import scene.GameObject;
/**
 * Gemi fiziği ve oyuncu kontrolünü sağlayan bileşen.
 * Su sürtünmesi (Drag), ivmelenme, dümen (Rudder) ve gaz kolu (Throttle)
 * dinamiklerini gerçekçi bir şekilde simüle eder.
 * Okyanus dalgalarına göre beşik hareketi (Bobbing) ve yatma (Roll/Pitch) yapar.
 */
public class ShipController extends Component {

	private extra.Camera camera;
	private terrain.flat.FlatTerrain terrain;
	private float time = 0.0f;
	
	// Dalga simülasyonu ayarları
	private float floatSpeed = 1.5f; // Dalganın inip çıkma hızı
	private float waveHeight = 0.4f; // Dalga yüksekliği
	
	// FİZİK - Gaz Kolu (Throttle) ve Hız
	private float targetThrottle = 0.0f;  // -1.0 (Tam Tornistan) ile 1.0 (Tam İleri) arası
	private float currentThrottle = 0.0f; // Motor devrinin gerçek zamanlı konumu
	private float currentSpeed = 0.0f;

	// --- GEMİ FİZİĞİ AYARLARI ---
	private float maxSpeedForward = 40.0f;
	private float maxSpeedBackward = 15.0f;
	private float engineAcceleration = 0.3f; // Gaz kolunun itilme hızı (daha yavaş)
	private float speedAcceleration = 4.0f; // Geminin hızlanma ivmesi (orijinali 10'du, daha ağır kalkış)
	private float waterDrag = 1.5f; // Suyun yavaşlatma etkisi (orijinali 5'ti, daha fazla süzülme/kayma)
	private float maxTurnRate = 20.0f; // Maksimum dönüş hızı (derece/saniye) (orijinali 25'ti)
	private float rudderSpeed = 1.0f; // Dümenin dönme hızı (orijinali 2.0'dı, daha ağır dümen)

	// FİZİK - Dümen (Rudder) ve Dönüş
	private float targetRudderAngle = 0.0f;  // -1.0 (Tam İskele/Sol) ile 1.0 (Tam Sancak/Sağ) arası
	private float currentRudderAngle = 0.0f; 
	
	// Görsel Efektler (Yatma / Roll ve Yunuslama / Pitch)
	private float currentRoll = 0.0f;
	private float currentPitch = 0.0f;
	
	private float basePitch;
	private float baseRoll;
	
	private float velocityYaw = 0.0f; // Su üzerindeki GİDİŞ yönümüz (Drift için)
	private boolean firstPhysicsTick = true;
	
	private float modelYawOffset = 0.0f; // GEMİ YÖNÜ HATALIYSA KALİBRASYON (J ve L Tuşlarıyla)
	private float waterHeight = 5.0f; // MainApp'teki okyanus yüksekliği
	
	private float hullDepthOffset = -6.4f; 
	
	private boolean isActive = false;
	
	public void setActive(boolean active) {
		this.isActive = active;
	}
	
	public void setModelYawOffset(float offset) {
		this.modelYawOffset = offset;
	}
	
	private java.util.List<scene.GameObject> propellers = new java.util.ArrayList<>();
	
	@Override
	public void start() {
		// Objeler sahnede ilk başladığında propeller (pervane) mesh'lerini bulalım
		if (gameObject != null && gameObject.getMultiMeshParts() != null) {
			for (scene.GameObject part : gameObject.getMultiMeshParts()) {
				if (part.getModel() != null && part.getModel().getModelData() != null) {
					String name = part.getModel().getModelData().getName();
					if (name != null) {
						String lower = name.toLowerCase();
						if (lower.contains("prop") || lower.contains("pervane") || lower.contains("rotor")) {
							propellers.add(part);
						}
					}
				}
			}
		}
	}

	public ShipController(extra.Camera camera, terrain.flat.FlatTerrain terrain) {
		this.camera = camera;
		this.terrain = terrain;
	}

	@Override
	public void start() {
		if (gameObject != null) {
			basePitch = gameObject.getRotation().x;
			baseRoll = gameObject.getRotation().z;
		}
	}

	@Override
	public void update(float delta) {
		if (gameObject == null) return;
		
		// Güvenlik: Başlangıçta delta çok büyükse fizik patlamasını önle
		if (delta > 0.05f) delta = 0.05f; 

		handleControls(delta);
		applyPhysics(delta);
		
		// 1. Fiziksel Y ekseni hareketi (Kaldırma Kuvveti - Basit Versiyon)
		float currentWaterLevel = waterHeight + (float) Math.sin(time) * waveHeight;
		float targetY = currentWaterLevel + hullDepthOffset; 
		
		// Yumuşak yüzme interpolasyonu
		float y = gameObject.getPosition().y;
		y += (targetY - y) * 2.0f * delta;
		gameObject.getPosition().y = y;
		
		// Dalga beşik efekti (Bobbing)
		float wavePitch = (float) Math.cos(time) * 1.5f;
		float waveRoll = (float) Math.sin(time * 0.8f) * 1.0f;
		
		gameObject.getRotation().x = basePitch + currentPitch + wavePitch;
		gameObject.getRotation().z = baseRoll + currentRoll + waveRoll;
		
		time += delta * floatSpeed;
		
		// Gemi Yönü Kalibrasyonu (J ve L Tuşları - Fiziksel Yön)
		if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_J)) {
			modelYawOffset -= 30.0f * delta;
			System.out.println("KALİBRASYON -> Gemi Fizik Yönü (Yaw): " + modelYawOffset);
		}
		if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_L)) {
			modelYawOffset += 30.0f * delta;
			System.out.println("KALİBRASYON -> Gemi Fizik Yönü (Yaw): " + modelYawOffset);
		}
		
		// Görsel Burun Kalibrasyonu (U ve O Tuşları - Görsel Yön)
		if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_U)) {
			gameObject.getModelOffsetRot().z -= 30.0f * delta;
			System.out.println("KALİBRASYON -> Gemi Görsel Yönü (Offset Z): " + gameObject.getModelOffsetRot().z);
		}
		if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_O)) {
			gameObject.getModelOffsetRot().z += 30.0f * delta;
			System.out.println("KALİBRASYON -> Gemi Görsel Yönü (Offset Z): " + gameObject.getModelOffsetRot().z);
		}
		
		// Derinlik Kalibrasyonu
		if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_UP)) {
			hullDepthOffset -= 5.0f * delta; 
			System.out.println("KALİBRASYON -> Gemi Batma Payı: " + hullDepthOffset);
		}
		if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_DOWN)) {
			hullDepthOffset += 5.0f * delta; 
			System.out.println("KALİBRASYON -> Gemi Batma Payı: " + hullDepthOffset);
		}
	}
	
	private void handleControls(float delta) {
		if (!isActive) {
			targetThrottle = 0.0f;
			targetRudderAngle = 0.0f;
		} else {
			// GAZ KOLU KONTROLÜ (Telegraph)
			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_W)) {
				targetThrottle = Math.min(1.0f, targetThrottle + delta * 0.5f); // Gaz kolunu ileri it
			} else if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_S)) {
				targetThrottle = Math.max(-1.0f, targetThrottle - delta * 0.5f); // Gaz kolunu geri çek
			}
			
			// DÜMEN KONTROLÜ (Rudder)
			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_A) || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LEFT)) {
				targetRudderAngle = 1.0f; // İskele (Sola Dönüş)
			} else if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_D) || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RIGHT)) {
				targetRudderAngle = -1.0f; // Sancak (Sağa Dönüş)
			} else {
				targetRudderAngle = 0.0f; // Dümeni ortala
			}
		}
		
		// Motor devri (currentThrottle) hedeflenen gaz koluna yavaşça ulaşır
		if (currentThrottle < targetThrottle) {
			currentThrottle = Math.min(targetThrottle, currentThrottle + engineAcceleration * delta);
		} else if (currentThrottle > targetThrottle) {
			currentThrottle = Math.max(targetThrottle, currentThrottle - engineAcceleration * delta);
		}
		
		// Dümen fiziki olarak dönme süresi ister (Anında dönmez)
		if (currentRudderAngle < targetRudderAngle) {
			currentRudderAngle = Math.min(targetRudderAngle, currentRudderAngle + rudderSpeed * delta);
		} else if (currentRudderAngle > targetRudderAngle) {
			currentRudderAngle = Math.max(targetRudderAngle, currentRudderAngle - rudderSpeed * delta);
		}
	}
	
	private void applyPhysics(float delta) {
		// HIZLANMA VE SÜRTÜNME (Drag)
		float targetSpeed = currentThrottle > 0 ? currentThrottle * maxSpeedForward : currentThrottle * maxSpeedBackward;
		
		// Su sürtünmesi ve pervanenin itme gücü (Hydrodynamics)
		if (currentSpeed < targetSpeed) {
			currentSpeed += speedAcceleration * delta;
			if (currentSpeed > targetSpeed) currentSpeed = targetSpeed;
		} else if (currentSpeed > targetSpeed) {
			currentSpeed -= (speedAcceleration + waterDrag) * delta; // Geri çekerken su da yardım eder
			if (currentSpeed < targetSpeed) currentSpeed = targetSpeed;
		}
		
		// DÖNÜŞ (Sadece gemi hareket ederken dönüş yapabilir)
		// Geminin dönüş yeteneği hızına bağlıdır. Dururken sıfırdır.
		float speedFactor = Math.abs(currentSpeed) / maxSpeedForward; 
		// Eğer gemi çok yavaşsa, dümene su çarpmadığı için dönmez. (Min hız eşiği)
		if (speedFactor < 0.01f) speedFactor = 0.01f; // Tamamen kilitlenmemesi için çok küçük bir tolerans
		
		float actualTurnRate = currentRudderAngle * maxTurnRate * speedFactor;
		
		// Geri geri giderken dönüş yönü tersine döner (Gerçekçi dümen davranışı)
		if (currentSpeed < -0.1f) {
			actualTurnRate = -actualTurnRate;
		}
		
		gameObject.getRotation().y += actualTurnRate * delta;
		
		// ----------------------------------------------------
		// Gerçekçi Gemi Fiziği: Momentum ve Drift (Kayma)
		// ----------------------------------------------------
		float targetYaw = gameObject.getRotation().y + modelYawOffset;
		
		// İlk başladığında gidiş yönünü direkt burnun yönüne eşitle
		if (firstPhysicsTick) {
			velocityYaw = targetYaw;
			firstPhysicsTick = false;
		}
		
		// Açı farkını bul (-180 ile 180 arasına sıkıştır)
		float yawDiff = targetYaw - velocityYaw;
		while (yawDiff > 180) yawDiff -= 360;
		while (yawDiff < -180) yawDiff += 360;
		
		// Gemi ne kadar hızlıysa (suya ne kadar tutunuyorsa), hareket yönü burnuna o kadar çabuk hizalanır
		float driftRecoveryRate = 1.0f + (speedFactor * 1.5f); // Hızlandıkça daha çabuk toparlar
		velocityYaw += yawDiff * driftRecoveryRate * delta;
		
		// YANA YATMA (Roll)
		// Dönüş dışına doğru merkezkaç kuvveti. yawDiff (kayma miktarı) arttıkça yatma artar.
		float targetRoll = -(yawDiff) * speedFactor * 0.3f;
		currentRoll += (targetRoll - currentRoll) * 2.0f * delta;
		
		// YUNUSLAMA (Pitch)
		// Hızlanırken burun kalkar, yavaşlarken veya fren yaparken burun düşer
		float accelerationForce = targetSpeed - currentSpeed;
		float targetPitch = -accelerationForce * 0.4f; 
		currentPitch += (targetPitch - currentPitch) * 2.0f * delta;
		
		// POZİSYON GÜNCELLEMESİ
		// Gemi burnuna (targetYaw) değil, su üstündeki süzülme/kayma yönüne (velocityYaw) doğru gider.
		float yawRad = (float) Math.toRadians(velocityYaw);
		
		float dx = (float) (-Math.sin(yawRad) * currentSpeed * delta);
		float dz = (float) (-Math.cos(yawRad) * currentSpeed * delta);
		
		// PERVANE ANİMASYONU
		// Gemi hareket ediyorsa pervaneleri döndür (Hız ile orantılı)
		if (!propellers.isEmpty() && Math.abs(currentSpeed) > 0.1f) {
			for (scene.GameObject prop : propellers) {
				// Genellikle pervaneler Z ekseninde (ileri/geri) döner, ancak modele göre X veya Y de olabilir.
				// Orijinal glb'ye göre bu eksen Z'dir.
				prop.getModelOffsetRot().z += currentSpeed * 40.0f * delta;
			}
		}
		
		// Şimdilik arazi çarpışması kapalı (Karaya oturma iptal)
		gameObject.getPosition().x += dx;
		gameObject.getPosition().z += dz;
	}
}
