package particles;

import org.lwjgl.util.vector.Vector3f;

/**
 * Tek bir parçacığı (partikül) temsil eden sınıf.
 * Tipine göre yerçekimi, hava direnci, su kaldırma kuvveti veya yüzey genişlemesi gibi 
 * fiziksel hareketleri kendi günceller.
 */
public class Particle {
	
	public Vector3f position;
	public Vector3f velocity;
	public float gravity;
	public float life;
	public float maxLife;
	public float scale;
	public Vector3f color;
	public float alpha;
	
	// Partikül tipi: 0 = SPRAY (Su damlası), 1 = FOAM (Köpük), 2 = BUBBLE (Kabarcık)
	public int type;

	public Particle(Vector3f position, Vector3f velocity, float gravity, float maxLife, float scale, Vector3f color, int type) {
		this.position = new Vector3f(
			sanitize(position.x),
			sanitize(position.y),
			sanitize(position.z)
		);
		this.velocity = new Vector3f(
			sanitize(velocity.x),
			sanitize(velocity.y),
			sanitize(velocity.z)
		);
		this.gravity = sanitize(gravity);
		this.life = 0f;
		this.maxLife = sanitize(maxLife);
		if (this.maxLife <= 0.0f) this.maxLife = 1.0f;
		this.scale = sanitize(scale);
		if (this.scale < 0.0f) this.scale = 0.0f;
		this.color = new Vector3f(color);
		this.alpha = 1.0f;
		this.type = type;
	}

	private float sanitize(float val) {
		if (Float.isNaN(val) || Float.isInfinite(val)) {
			return 0.0f;
		}
		return val;
	}

	/**
	 * Parçacığın fizik durumunu günceller.
	 * @param delta Son kareden beri geçen süre (saniye)
	 * @param waterHeight Parçacığın bulunduğu X/Z koordinatındaki su yüzeyi yüksekliği
	 * @return Parçacık yaşıyorsa true, ömrü bittiyse false
	 */
	public boolean update(float delta, float waterHeight) {
		if (Float.isNaN(delta) || Float.isInfinite(delta) || delta <= 0.0f) {
			return true; // Skip this frame but keep particle alive
		}

		if (Float.isNaN(waterHeight) || Float.isInfinite(waterHeight)) {
			waterHeight = -2.0f;
		}

		life += delta;
		if (life >= maxLife) {
			return false; // Ömrünü doldurdu
		}

		float lifeRatio = life / maxLife;
		if (lifeRatio < 0.0f) lifeRatio = 0.0f;
		if (lifeRatio > 1.0f) lifeRatio = 1.0f;

		if (type == 0) { // SPRAY (Havaya sıçrayan su damlası)
			// Yerçekimi ivmesini uygula
			velocity.y += gravity * delta;
			position.x += velocity.x * delta;
			position.y += velocity.y * delta;
			position.z += velocity.z * delta;

			// Ömrünün sonuna doğru görünmezliğe geç (fade out)
			alpha = 1.0f - lifeRatio;
			
			// Yarı ömürden sonra küçül
			if (lifeRatio > 0.5f) {
				scale *= (1.0f - delta * 2.0f);
			}

			// Su yüzeyinin altına düşerse yok et
			if (velocity.y < 0 && position.y < waterHeight) {
				return false;
			}
		} 
		else if (type == 1) { // FOAM (Su yüzeyinde yayılan köpük halkası)
			// Yavaşça akıntı veya itme hızıyla sürüklen
			position.x += velocity.x * delta;
			position.z += velocity.z * delta;
			
			// Tam su dalga yüksekliğinde kalmasını sağla
			position.y = waterHeight + 0.05f;

			// Köpük yavaşça genişler ve solar
			scale += delta * 0.4f;
			alpha = (float) Math.pow(1.0f - lifeRatio, 1.5f);
		} 
		else if (type == 2) { // BUBBLE (Su altında yükselen hava kabarcıkları)
			// Sabit hızla yukarı doğru yüz
			velocity.y = 1.8f;
			position.x += velocity.x * delta;
			position.y += velocity.y * delta;
			position.z += velocity.z * delta;

			// Su yüzeyine yaklaştıkça hafifçe sağa sola yalpalasın
			position.x += (float) Math.sin(life * 15.0f) * 0.04f * delta;

			// Saydamlık zamanla sönümlensin
			alpha = 0.8f * (1.0f - lifeRatio);

			// Su yüzeyine ulaştığı an patlasın
			if (position.y >= waterHeight) {
				return false;
			}
		}
		else if (type == 3) { // WIND_LINE (Rüzgar çizgisi)
			position.x += velocity.x * delta;
			position.y += velocity.y * delta;
			position.z += velocity.z * delta;

			// Belirme ve kaybolma efekti (Fade in & out)
			if (lifeRatio < 0.2f) {
				alpha = lifeRatio / 0.2f; // %0'dan %20'ye kadar yavaşça belirir
			} else if (lifeRatio > 0.8f) {
				alpha = (1.0f - lifeRatio) / 0.2f; // %80'den sonra yavaşça kaybolur
			} else {
				alpha = 1.0f;
			}
		}

		// Her ihtimale karşı tüm değerleri tekrar temizle
		position.x = sanitize(position.x);
		position.y = sanitize(position.y);
		position.z = sanitize(position.z);
		velocity.x = sanitize(velocity.x);
		velocity.y = sanitize(velocity.y);
		velocity.z = sanitize(velocity.z);
		scale = sanitize(scale);
		if (scale < 0.0f) scale = 0.0f;
		alpha = sanitize(alpha);
		if (alpha < 0.0f) alpha = 0.0f;
		if (alpha > 1.0f) alpha = 1.0f;

		return true;
	}
}
