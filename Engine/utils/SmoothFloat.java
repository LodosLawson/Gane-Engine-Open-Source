package utils;

/**
 * Belirli bir değeri (target) zaman içerisinde (delta) yumuşak (smooth) bir şekilde yakalamaya çalışan
 * hareket/geçiş mantığı sınıfı.
 * Genellikle kamera hareketleri, interpolasyonlar veya yumuşak UI geçişleri için kullanılır.
 * 
 * <p><b>Kullanım Örneği:</b></p>
 * <pre>
 * {@code
 * // Başlangıç değeri 0, çeviklik (agility) 10 olan bir SmoothFloat oluştur.
 * SmoothFloat fov = new SmoothFloat(0f, 10f);
 * 
 * // Hedefi 90 olarak belirle.
 * fov.setTarget(90f);
 * 
 * // Oyun döngüsü içerisinde (her frame'de) güncelle
 * fov.update(DisplayManager.getFrameTime());
 * 
 * // O anki yumuşatılmış değeri al
 * float currentFov = fov.get();
 * }
 * </pre>
 */
public class SmoothFloat {
	
	/** Hedefe yaklaşma çevikliği / hızı. Değer arttıkça hedef değere daha hızlı yaklaşır. */
	private final float agility;
	
	/** Ulaşılmak istenen hedef değer. */
	private float target;
	
	/** O anki mevcut (güncel) değer. */
	private float actual;
	
	/**
	 * SmoothFloat nesnesini belirtilen başlangıç değeri ve çeviklik ile başlatır.
	 * 
	 * @param initialValue Başlangıç ve ilk hedef değeri.
	 * @param agility Yaklaşma çevikliği (Hız çarpanı, örn: 10f).
	 */
	public SmoothFloat(float initialValue, float agility){
		this.target = initialValue;
		this.actual = initialValue;
		this.agility = agility;
	}
	
	/**
	 * Zaman adımı (delta time) kullanarak mevcut değeri hedefe doğru yumuşakça günceller.
	 * Her render karesinde (frame) bir kez çağrılması gerekir.
	 * 
	 * @param delta İki kare arası geçen zaman (saniye cinsinden delta time).
	 */
	public void update(float delta){
		float offset = target - actual; // Farkı bul
		float change = offset * delta * agility; // Değişim miktarını hesapla
		actual += change; // Mevcut değeri yavaşça güncelle
	}
	
	/**
	 * Hedef değeri (target) belirtilen miktar kadar arttırır.
	 * 
	 * @param dT Hedef değere eklenecek miktar.
	 */
	public void increaseTarget(float dT){
		this.target += dT;
	}
	
	/**
	 * Yeni bir hedef değer belirler. Mevcut değer (actual), bu yeni hedefe doğru 
	 * update() fonksiyonu çağrıldıkça yavaşça yaklaşacaktır.
	 * 
	 * @param target Yeni ulaşılması istenen hedef değer.
	 */
	public void setTarget(float target){
		this.target = target;
	}
	
	/**
	 * Mevcut değeri (actual) anında doğrudan arttırır. Herhangi bir yumuşatma 
	 * uygulanmaz, anlık (instant) bir değişim olur.
	 * 
	 * @param increase Eklenecek anlık miktar.
	 */
	public void instantIncrease(float increase){
		this.actual += increase;
	}
	
	/** 
	 * O anki mevcut/güncel değeri döndürür.
	 * 
	 * @return O anki mevcut (yumuşatılmış) değer.
	 */
	public float get(){
		return actual;
	}
	
	/**
	 * Mevcut değeri (actual) zorla (force) doğrudan hedef değere (target) eşitler.
	 * Böylece geçiş anında tamamlanmış olur.
	 */
	public void force(){
		this.actual = this.target;
	}
	
	/** 
	 * Ulaşılmaya çalışılan hedef değeri (target) döndürür.
	 * 
	 * @return Hedef değer.
	 */
	public float getTarget(){
		return target;
	}

}
