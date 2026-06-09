package scene;

/**
 * GameObject'lere animasyon ekleyen modüler bileşen.
 * Hem sürekli (her karede) dönen/sallanan otomatik animasyonları
 * hem de bir tuşa basıldığında tetiklenen özel efekt animasyonlarını yönetir.
 */
public class AnimatorComponent extends Component {

	// Sürekli kendi etrafında dönme hızları (Derece/Saniye)
	private float rotSpeedX = 0f;
	private float rotSpeedY = 0f;
	private float rotSpeedZ = 0f;
	
	// Yukarı/aşağı dalgalanma (hover/bounce) efekti
	private boolean bounceEffect = false;
	private float bounceSpeed = 2f;
	private float bounceAmount = 0.5f;
	private float bounceTime = 0f;
	private float startY = 0f;

	// Tetiklenen aksiyon animasyonu (Örn: Spin & Büyüme)
	private boolean triggerActive = false;
	private float triggerTime = 0f;
	private float triggerDuration = 1.0f; // 1 saniye sürer
	
	public AnimatorComponent() {}

	/** Sürekli dönme animasyon hızlarını ayarlar */
	public void setContinuousRotation(float speedX, float speedY, float speedZ) {
		this.rotSpeedX = speedX;
		this.rotSpeedY = speedY;
		this.rotSpeedZ = speedZ;
	}

	/** Yukarı aşağı dalgalanma (hover) efektini ayarlar */
	public void setBounceEffect(float speed, float amount) {
		this.bounceEffect = true;
		this.bounceSpeed = speed;
		this.bounceAmount = amount;
	}

	/** Tetiklemeli animasyonu başlatır */
	public void triggerAction() {
		this.triggerActive = true;
		this.triggerTime = 0f;
	}

	@Override
	public void start() {
		if (gameObject != null) {
			this.startY = gameObject.getPosition().y;
		}
	}

	@Override
	public void update(float delta) {
		if (gameObject == null) return;

		// 1. Otomatik/Sürekli Dönme Animasyonu (Her saniye çalışır)
		gameObject.getRotation().x += rotSpeedX * delta;
		gameObject.getRotation().y += rotSpeedY * delta;
		gameObject.getRotation().z += rotSpeedZ * delta;
		
		gameObject.getRotation().x %= 360;
		gameObject.getRotation().y %= 360;
		gameObject.getRotation().z %= 360;

		// 2. Otomatik/Sürekli Yukarı-Aşağı Dalgalanma (Hover)
		if (bounceEffect) {
			bounceTime += delta * bounceSpeed;
			float offset = (float) Math.sin(bounceTime) * bounceAmount;
			gameObject.getPosition().y = startY + offset;
		}

		// 3. Tetiklenen Aksiyon Animasyonu (Tetiklenince hızlıca spin atıp büyür/küçülür)
		if (triggerActive) {
			triggerTime += delta;
			if (triggerTime < triggerDuration) {
				float progress = triggerTime / triggerDuration; // 0.0 -> 1.0
				
				// Sinüs dalgası ile yumuşak büyüme/küçülme (0 -> +0.5 -> 0)
				float scaleMultiplier = 1.0f + (float) Math.sin(progress * Math.PI) * 0.5f;
				gameObject.setScale(scaleMultiplier);
				
				// Tetikleme esnasında ekstra hızlı dönme efekti
				gameObject.getRotation().y += 720f * delta; 
			} else {
				triggerActive = false;
				gameObject.setScale(1.0f); // Boyutu normale döndür
			}
		}
	}
}
