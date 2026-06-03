package default_controls;

import scene.Component;
import scene.GameObject;
/**
 * Denizdeki rastgele yüzen balıkları (AI) kontrol eden bileşen.
 * Balıkların rastgele hız, derinlik ve yönde yüzmelerini;
 * aynı zamanda suyun dalgasına göre inip çıkmalarını (bobbing) sağlar.
 */
public class FishController extends Component {

	private float speed = 10.0f;
	private float turnSpeed = 40.0f;
	
	private float time = 0;
	private float floatSpeed = 2.0f;
	private float waveHeight = 0.5f;
	
	private float waterHeight = 4.5f; // Su seviyesi
	private float currentDepth = 2.0f; // Suyun altında yüzeceği varsayılan derinlik
	private float targetTurnOffset = 0;

	@Override
	public void start() {
		// Rastgele bir yöne baksın
		if (gameObject != null) {
			gameObject.getRotation().y = (float) (Math.random() * 360.0);
			currentDepth = (float) (1.0 + Math.random() * 4.0); // 1.0 ile 5.0 birim arası derinlik
			speed = (float) (15.0 + Math.random() * 10.0); // 15 ile 25 arası hız (gemiyi yakalayabilsin)
			time = (float) (Math.random() * 100f);
		}
	}

	@Override
	public void update(float delta) {
		if (gameObject == null) return;
		
		time += delta;
		
		// Rastgele dönüş manevraları (yapay zeka)
		if (Math.random() < 0.01) { // %1 ihtimalle her karede yön değiştir
			targetTurnOffset = (float) ((Math.random() - 0.5) * 180.0); // -90 ile +90 derece arası
		}
		
		// Dönüşü uygula (Yumuşak dönüş)
		if (targetTurnOffset > 1.0f) {
			gameObject.getRotation().y += turnSpeed * delta;
			targetTurnOffset -= turnSpeed * delta;
		} else if (targetTurnOffset < -1.0f) {
			gameObject.getRotation().y -= turnSpeed * delta;
			targetTurnOffset += turnSpeed * delta;
		}
		
		// Balığın baktığı yöne doğru ilerle
		float yawRad = (float) Math.toRadians(gameObject.getRotation().y);
		float dx = (float) (Math.sin(yawRad) * speed * delta);
		float dz = (float) (-Math.cos(yawRad) * speed * delta);
		
		gameObject.getPosition().x += dx;
		gameObject.getPosition().z += dz;
		
		// Suda aşağı yukarı süzülme (Bobbing)
		float yOffset = (float) Math.sin(time * floatSpeed) * waveHeight;
		gameObject.getPosition().y = waterHeight - currentDepth + yOffset;
		
		// İleri geri yunuslama (Yüzerken burnunu aşağı yukarı yapma efekti)
		float pitchOffset = (float) Math.cos(time * floatSpeed) * 15.0f;
		gameObject.getRotation().x = pitchOffset;
		
		// Kuyruk sallama veya yana yatma efekti (Opsiyonel)
		float rollOffset = (float) Math.sin(time * floatSpeed * 2.0f) * 10.0f;
		gameObject.getRotation().z = rollOffset;
	}

}
