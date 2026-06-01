package scene;

import java.util.Random;
import org.lwjgl.util.vector.Vector3f;
import terrain.flat.FlatTerrain;

/**
 * Yapay zeka ile etrafta rastgele uçuşan diğer kuşları kontrol eden bileşen.
 * Kendi başına kararlar alır, hedef noktalar seçer ve oraya doğru yönelip uçar.
 */
public class BirdAIController extends Component {

	private final FlatTerrain terrain;
	private final Random random = new Random();

	private float moveSpeed = 22.0f; // AI kuşları oyuncudan biraz daha yavaş uçarlar
	private float gravity = -20.0f;

	private Vector3f targetPosition = new Vector3f();
	private boolean hasTarget = false;

	// Model hizalama offsetleri (Oyuncu kuşuyla aynı)
	private float modelYawOffset = 180.0f;
	private float modelPitchOffset = -90.0f;
	private float modelRollOffset = 0.0f;

	// AI Uçuş Yükseklik limitleri
	private float minFlightHeight = 35.0f;
	private float maxFlightHeight = 100.0f;
	private float wanderRange = 350.0f; // Her adımda en fazla ne kadar uzağa yeni hedef seçeceği

	public BirdAIController(FlatTerrain terrain) {
		this.terrain = terrain;
	}

	@Override
	public void start() {
		if (gameObject != null) {
			gameObject.setCullingRadius(500.0f); // Kameralardan aniden kaybolmasını engelle
			chooseNewTarget();
		}
	}

	@Override
	public void update(float delta) {
		if (gameObject == null) return;

		float x = gameObject.getPosition().x;
		float y = gameObject.getPosition().y;
		float z = gameObject.getPosition().z;

		// 1. Hedefe ulaştıysak veya hedefimiz yoksa yeni hedef seç
		float dx = targetPosition.x - x;
		float dy = targetPosition.y - y;
		float dz = targetPosition.z - z;
		float distToTarget = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

		if (!hasTarget || distToTarget < 12.0f) {
			chooseNewTarget();
			// Yeni hedef mesafesini güncelle
			dx = targetPosition.x - x;
			dy = targetPosition.y - y;
			dz = targetPosition.z - z;
			distToTarget = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		}

		// 2. Hedefe doğru yönel ve uç
		if (distToTarget > 0) {
			float dirX = dx / distToTarget;
			float dirY = dy / distToTarget;
			float dirZ = dz / distToTarget;

			x += dirX * moveSpeed * delta;
			y += dirY * moveSpeed * delta;
			z += dirZ * moveSpeed * delta;

			// Arazi yüksekliğini kontrol et ve çarpmaktan kaçın
			float groundHeight = terrain.getHeightAt(x, z);
			if (y < groundHeight + 10.0f) {
				y = groundHeight + 10.0f;
				chooseNewTarget(); // Çarpmak üzereysek rotayı değiştir
			}

			gameObject.getPosition().set(x, y, z);

			// 3. Yönelim Açılarını Hesapla (Doğrultuya göre dönüş)
			// Yatay dönüş açısı (Yaw)
			float targetYaw = (float) Math.toDegrees(Math.atan2(dirX, -dirZ));
			float currentYaw = gameObject.getRotation().y;
			float targetVisualYaw = targetYaw + modelYawOffset;
			float diffYaw = targetVisualYaw - currentYaw;
			while (diffYaw < -180) diffYaw += 360;
			while (diffYaw > 180) diffYaw -= 360;
			gameObject.getRotation().y = currentYaw + diffYaw * 4f * delta;

			// Dikey eğim açısı (Pitch)
			float targetPitch = (float) Math.toDegrees(Math.asin(-dirY));
			float currentPitch = gameObject.getRotation().x;
			float targetVisualPitch = targetPitch + modelPitchOffset;
			float diffPitch = targetVisualPitch - currentPitch;
			gameObject.getRotation().x = currentPitch + diffPitch * 4f * delta;
			
			gameObject.getRotation().z = modelRollOffset;
		}

		// 4. Animasyonu Sürekli Çözümle (Uçuş Animasyonu)
		scene.animation.Animator animator = gameObject.getAnimator();
		if (animator != null) {
			animator.resume();
			animator.setSpeed(1.0f);
		}
	}

	private void chooseNewTarget() {
		if (gameObject == null) return;
		float cx = gameObject.getPosition().x;
		float cz = gameObject.getPosition().z;

		// Yeni hedef koordinatları
		float tx = cx + (random.nextFloat() * 2.0f - 1.0f) * wanderRange;
		float tz = cz + (random.nextFloat() * 2.0f - 1.0f) * wanderRange;

		// Sınırları aşmasını engelle
		if (tx < 100) tx = 100;
		if (tx > 1900) tx = 1900;
		if (tz < 100) tz = 100;
		if (tz > 1900) tz = 1900;

		float groundHeight = terrain.getHeightAt(tx, tz);
		float ty = groundHeight + minFlightHeight + (random.nextFloat() * (maxFlightHeight - minFlightHeight));

		targetPosition.set(tx, ty, tz);
		hasTarget = true;
	}

	// Offset Özelleştirme Getters & Setters
	public void setModelYawOffset(float offset) { this.modelYawOffset = offset; }
	public void setModelPitchOffset(float offset) { this.modelPitchOffset = offset; }
	public void setModelRollOffset(float offset) { this.modelRollOffset = offset; }
}
