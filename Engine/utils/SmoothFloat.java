package utils;

/**
 * Belirli bir değeri (target) zaman içerisinde (delta) yumuşak (smooth) bir şekilde yakalamaya çalışan
 * hareket/geçiş mantığı sınıfı.
 * Genellikle kamera hareketleri, interpolasyonlar veya yumuşak UI geçişleri için kullanılır.
 */
public class SmoothFloat {
	
	// Hedefe yaklaşma çevikliği / hızı. Değer arttıkça daha hızlı yaklaşır.
	private final float agility;
	
	// Ulaşılmak istenen hedef değer
	private float target;
	// O anki mevcut değer
	private float actual;
	
	/**
	 * SmoothFloat nesnesini başlatır.
	 * 
	 * @param initialValue Başlangıç ve hedef değeri
	 * @param agility Yaklaşma çevikliği (Hız çarpanı)
	 */
	public SmoothFloat(float initialValue, float agility){
		this.target = initialValue;
		this.actual = initialValue;
		this.agility = agility;
	}
	
	/**
	 * Zaman adımı (delta time) kullanarak mevcut değeri hedefe doğru yumuşakça günceller.
	 * Her karede (frame) çağrılması gerekir.
	 * 
	 * @param delta İki kare arası geçen zaman
	 */
	public void update(float delta){
		float offset = target - actual; // Farkı bul
		float change = offset * delta * agility; // Değişim miktarını hesapla
		actual += change; // Mevcut değeri yavaşça güncelle
	}
	
	/**
	 * Hedef değeri belirli bir miktar arttırır.
	 * @param dT Eklenecek miktar
	 */
	public void increaseTarget(float dT){
		this.target += dT;
	}
	
	/**
	 * Yeni bir hedef değer belirler.
	 * @param target Yeni ulaşılması istenen değer
	 */
	public void setTarget(float target){
		this.target = target;
	}
	
	/**
	 * Mevcut değeri (actual) anında yumuşaklık aramadan doğrudan arttırır.
	 * @param increase Eklenecek anlık miktar
	 */
	public void instantIncrease(float increase){
		this.actual += increase;
	}
	
	/** @return O anki mevcut/güncel değer */
	public float get(){
		return actual;
	}
	
	// Kullanılmayan/Boş bırakılmış metod
	public void force(){}
	
	/** @return Ulaşılmaya çalışılan hedef değer */
	public float getTarget(){
		return target;
	}

}
