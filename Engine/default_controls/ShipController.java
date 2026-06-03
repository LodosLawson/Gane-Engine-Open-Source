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
	private float maxSpeedForward = 40.0f;
	private float maxSpeedBackward = 15.0f;
	private float currentSpeed = 0.0f;
	
	// İvmelenme ve Su Sürtünmesi (Drag)
	private float engineAcceleration = 2.0f; // Motor devrinin (throttle) değişme hızı
	private float speedAcceleration = 5.0f;  // Geminin gaza tepki verme hızı (Büyük gemiler yavaş hızlanır)
	private float waterDrag = 1.5f;          // Su sürtünmesi, gaz kapalıyken yavaşlamayı sağlar

	// FİZİK - Dümen (Rudder) ve Dönüş
	private float targetRudderAngle = 0.0f;  // -1.0 (Tam İskele/Sol) ile 1.0 (Tam Sancak/Sağ) arası
	private float currentRudderAngle = 0.0f; 
	private float rudderSpeed = 2.0f;        // Dümenin dönme hızı
	private float maxTurnRate = 25.0f;       // Saniyede max dönme derecesi (Tam hızda)
	
	// Görsel Efektler (Yatma / Roll ve Yunuslama / Pitch)
	private float currentRoll = 0.0f;
	private float currentPitch = 0.0f;
	
	private float basePitch;
	private float baseRoll;
	
	private float modelYawOffset = 0.0f; // GEMİ YÖNÜ HATALIYSA KALİBRASYON (J ve L Tuşlarıyla)
	private float waterHeight = 5.0f; // MainApp'teki okyanus yüksekliği
	
	private float hullDepthOffset = -6.4f; 

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
		
		// Gemi Yönü Kalibrasyonu (J ve L Tuşları)
		if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_J)) {
			modelYawOffset -= 30.0f * delta;
			System.out.println("KALİBRASYON -> Gemi Yönü Ofseti (Yaw): " + modelYawOffset);
		}
		if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_L)) {
			modelYawOffset += 30.0f * delta;
			System.out.println("KALİBRASYON -> Gemi Yönü Ofseti (Yaw): " + modelYawOffset);
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
		// GAZ KOLU KONTROLÜ (Telegraph)
		if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_W)) {
			targetThrottle = Math.min(1.0f, targetThrottle + delta * 0.5f); // Gaz kolunu ileri it
		} else if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_S)) {
			targetThrottle = Math.max(-1.0f, targetThrottle - delta * 0.5f); // Gaz kolunu geri çek
		}
		
		// Motor devri (currentThrottle) hedeflenen gaz koluna yavaşça ulaşır
		if (currentThrottle < targetThrottle) {
			currentThrottle = Math.min(targetThrottle, currentThrottle + engineAcceleration * delta);
		} else if (currentThrottle > targetThrottle) {
			currentThrottle = Math.max(targetThrottle, currentThrottle - engineAcceleration * delta);
		}
		
		// DÜMEN KONTROLÜ (Rudder)
		if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_A) || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LEFT)) {
			targetRudderAngle = 1.0f; // İskele (Sola Dönüş)
		} else if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_D) || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RIGHT)) {
			targetRudderAngle = -1.0f; // Sancak (Sağa Dönüş)
		} else {
			targetRudderAngle = 0.0f; // Dümeni ortala
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
		
		// YANA YATMA (Roll)
		// Dönüş dışına doğru merkezkaç kuvveti. Hız ve dönüş oranıyla artar.
		float targetRoll = -(actualTurnRate * speedFactor) * 0.4f;
		currentRoll += (targetRoll - currentRoll) * 2.0f * delta;
		
		// YUNUSLAMA (Pitch)
		// Hızlanırken burun kalkar, yavaşlarken veya fren yaparken burun düşer
		float accelerationForce = targetSpeed - currentSpeed;
		float targetPitch = -accelerationForce * 0.5f; 
		currentPitch += (targetPitch - currentPitch) * 2.0f * delta;
		
		// POZİSYON GÜNCELLEMESİ
		float movementYaw = gameObject.getRotation().y + modelYawOffset;
		float yawRad = (float) Math.toRadians(movementYaw);
		
		float dx = (float) (Math.sin(yawRad) * currentSpeed * delta);
		float dz = (float) (-Math.cos(yawRad) * currentSpeed * delta);
		
		// Şimdilik arazi çarpışması kapalı (Karaya oturma iptal)
		gameObject.getPosition().x += dx;
		gameObject.getPosition().z += dz;
	}
}
