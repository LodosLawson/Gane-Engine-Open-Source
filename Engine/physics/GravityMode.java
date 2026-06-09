package physics;

/**
 * Fizik motorunun yerçekimi davranış modlarını belirler.
 */
public enum GravityMode {
	
	/**
	 * Standart yönsel yerçekimi. Belirlenen globalGravity vektörü yönünde çeker (Örn: Dünyada aşağıya doğru).
	 */
	DIRECTIONAL,
	
	/**
	 * Sıfır yerçekimi. Uzay boşluğu gibi, hiçbir çekim kuvveti uygulanmaz. Sadece itme kuvvetleri (ivme) ile hareket edilir.
	 */
	ZERO_GRAVITY,
	
	/**
	 * Gezegensel yerçekimi. Objeler düz bir çizgi halinde aşağı düşmek yerine, belirlenen bir merkeze (gezegenin çekirdeğine) doğru çekilir.
	 */
	PLANETARY

}
