package gane;

/**
 * Geliştiricinin oyununa özel ayarları (Çözünürlük, Tam Ekran durumu,
 * Pencere Başlığı, İkon vb.) yapılandırabileceği merkezi ayar sınıfı.
 * Motor (Engine) bu ayarları okuyarak kendini yapılandırır.
 */
public class AppSettings {

	// --- Varsayılan Ayarlar ---
	public static int width = 1280;
	public static int height = 720;
	public static boolean fullscreen = false;
	public static String title = "Gane Engine Game";
	public static String logoPath = null;

	/**
	 * Geliştirici oyununu başlatmadan önce bu fonksiyonu çağırarak
	 * motorun tüm pencerelerini ve genel ayarlarını tek bir yerden özelleştirir.
	 */
	public static void setup(int w, int h, boolean fs, String gameTitle, String logo) {
		width = w;
		height = h;
		fullscreen = fs;
		title = gameTitle;
		logoPath = logo;
	}

	/**
	 * Hızlı kurulum için kolaylaştırıcı fonksiyon.
	 */
	public static void initDefault() {
		width = 1280;
		height = 720;
		fullscreen = false;
		title = "Gane Engine Game";
		logoPath = null;
	}
}
