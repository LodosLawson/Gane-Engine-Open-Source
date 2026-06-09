package steam;

import com.codedisaster.steamworks.SteamAPI;
import com.codedisaster.steamworks.SteamException;

/**
 * Oyun motorunun Steam API ile haberleşmesini sağlayan ana yönetici.
 * Başarımlar, arkadaş listesi, P2P bağlantılar gibi Steam servisleri için motoru hazırlar.
 */
public class SteamManager {

	private static boolean isInitialized = false;

	/**
	 * Steam API'yi başlatır. Oyunun başında çağrılmalıdır.
	 * 
	 * @return Steam arka planda açıksa ve API başarıyla yüklendiyse true.
	 */
	public static boolean init() {
		try {
			// steamworks4j native kütüphanelerini yükle (steam_api64.dll vb.)
			SteamAPI.loadLibraries();
			
			// steamworks4j kütüphanesi başlatılıyor
			if (SteamAPI.init()) {
				isInitialized = true;
				System.out.println("Steam API basariyla baslatildi.");
				return true;
			}
		} catch (SteamException e) {
			System.err.println("Steam API baslatilirken hata olustu: " + e.getMessage());
		}
		
		System.out.println("Steam baglantisi kurulamadi. Oyun offline modda calisacak.");
		return false;
	}

	/**
	 * Oyun döngüsünün (Game Loop) içinde her karede (frame) çağrılmalıdır.
	 * Steam'den gelen davetleri, başarımları ve mesajları (Callback'ler) işler.
	 */
	public static void update() {
		if (isInitialized) {
			SteamAPI.runCallbacks();
		}
	}

	/**
	 * Oyun kapanırken Steam bağlantısını güvenli bir şekilde keser.
	 */
	public static void shutdown() {
		if (isInitialized) {
			SteamAPI.shutdown();
			isInitialized = false;
			System.out.println("Steam API kapatildi.");
		}
	}

	public static boolean isSteamRunning() {
		return isInitialized;
	}
}
