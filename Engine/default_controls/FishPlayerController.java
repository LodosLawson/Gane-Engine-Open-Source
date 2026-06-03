package default_controls;

import scene.Component;
import scene.GameObject;
import org.lwjgl.input.Keyboard;

/**
 * Oyuncu tarafından kontrol edilen balık bileşeni.
 * WASD/Yön tuşları ile yön ve hız, SPACE/LSHIFT ile derinlik ayarı yapılır.
 * Dönüşlerde balığın fiziksel olarak yana yatmasını (Roll) ve 
 * yüzerken burnunu aşağı eğmesini (Pitch) sağlar.
 */
public class FishPlayerController extends Component {

	private extra.Camera camera;
	private float speed = 25.0f;
	private float turnSpeed = 80.0f;
	private float currentSpeed = 0.0f;
	private float acceleration = 10.0f;
	
	private float time = 0;
	private float floatSpeed = 4.0f;
	private float waterHeight = 4.8f; // Su yüzeyi seviyesi
	private float currentDepth = 2.0f; // Başlangıç derinliği
	
	private float modelYawOffset = 0.0f;
	private float modelPitchOffset = 0.0f;
	private float modelRollOffset = 0.0f;

	public FishPlayerController(extra.Camera camera) {
		this.camera = camera;
	}

	@Override
	public void start() {
	}

	@Override
	public void update(float delta) {
		if (gameObject == null) return;
		
		time += delta;

		// Yön Kontrolü (A / D)
		if (Keyboard.isKeyDown(Keyboard.KEY_A) || Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
			gameObject.getRotation().y += turnSpeed * delta;
		} else if (Keyboard.isKeyDown(Keyboard.KEY_D) || Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
			gameObject.getRotation().y -= turnSpeed * delta;
		}

		// Hız Kontrolü (W / S)
		float targetSpeed = 0.0f;
		if (Keyboard.isKeyDown(Keyboard.KEY_W) || Keyboard.isKeyDown(Keyboard.KEY_UP)) {
			targetSpeed = speed;
		} else if (Keyboard.isKeyDown(Keyboard.KEY_S) || Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
			targetSpeed = -speed * 0.5f;
		}
		
		if (currentSpeed < targetSpeed) {
			currentSpeed += acceleration * delta;
		} else if (currentSpeed > targetSpeed) {
			currentSpeed -= acceleration * delta;
		}

		// Derinlik Kontrolü (SPACE yüzeye çık, SHIFT dibe in)
		if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
			currentDepth -= 15.0f * delta; // Yüksel
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
			currentDepth += 15.0f * delta; // Alçal
		}
		
		// Derinlik Sınırları (0 = Su yüzeyi, 30 = Maksimum derinlik)
		if (currentDepth < 0.5f) currentDepth = 0.5f; // Suyun dışına çıkamasın
		if (currentDepth > 30.0f) currentDepth = 30.0f;

		// Hareket (Pozisyonu Güncelle)
		float yawRad = (float) Math.toRadians(gameObject.getRotation().y + modelYawOffset);
		float dx = (float) (Math.sin(yawRad) * currentSpeed * delta);
		float dz = (float) (-Math.cos(yawRad) * currentSpeed * delta); // İleri yön Z ekseni
		
		gameObject.getPosition().x += dx;
		gameObject.getPosition().z += dz;

		// Y ekseni (Suyun Altı)
		// Yüzme animasyonu (Bobbing)
		float yOffset = (float) Math.sin(time * floatSpeed) * 0.2f;
		gameObject.getPosition().y = waterHeight - currentDepth + yOffset;

		// Görsel Eğim (Pitch & Roll)
		// Balık hareket ederken burnunu hafif aşağı eğer
		float movementPitch = currentSpeed * 0.5f; 
		float bobbingPitch = (float) Math.cos(time * floatSpeed) * 10.0f;
		gameObject.getRotation().x = bobbingPitch + movementPitch + modelPitchOffset;
		
		// Dönüşlerde yana yatma (Roll)
		float targetRoll = 0.0f;
		if (Keyboard.isKeyDown(Keyboard.KEY_A) || Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
			targetRoll = -20.0f;
		} else if (Keyboard.isKeyDown(Keyboard.KEY_D) || Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
			targetRoll = 20.0f;
		}
		// Roll değerini yumuşak şekilde uygula
		gameObject.getRotation().z += (targetRoll - gameObject.getRotation().z) * 5.0f * delta + modelRollOffset;
	}
	
	public void setModelYawOffset(float offset) { this.modelYawOffset = offset; }
	public void setModelPitchOffset(float offset) { this.modelPitchOffset = offset; }
	public void setModelRollOffset(float offset) { this.modelRollOffset = offset; }
}
