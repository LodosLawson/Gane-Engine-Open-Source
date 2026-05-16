package utils;

import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.PixelFormat;

/**
 * Oyunun ana penceresinin (Display) oluşturulmasını, güncellenmesini ve kapatılmasını sağlayan yönetici sınıf.
 * Ayrıca oyun döngüsü için geçen süreyi (Delta Time) hesaplar.
 */
public class DisplayManager {
	
	// Pencere başlığı
	private static final String TITLE = "Socuwan Scene";
	// Pencere genişliği
	private static final int WIDTH = 1280;
	// Pencere yüksekliği
	private static final int HEIGHT = 720;
	// Hedeflenen maksimum saniye başına kare sayısı (FPS)
	private static final int FPS_CAP = 100;
	
	// Bir önceki karenin çizildiği zaman (Milisaniye)
	private static long lastFrameTime;
	// İki kare arasında geçen süre (Delta Time - Saniye cinsinden)
	private static float delta;
	
	/**
	 * OpenGL penceresini belirtilen özelliklerde oluşturur.
	 * Antialiasing (Multisample) ve varsayılan viewport ayarlarını yapar.
	 * 
	 * @return Yeni bir DisplayManager nesnesi
	 */
	public static DisplayManager createDisplay(){
		try {
			Display.setDisplayMode(new DisplayMode(WIDTH,HEIGHT));
			// Depth buffer bit derinliği 24 ve Antialiasing (Multisample) seviyesi 4 olarak ayarlanır
			Display.create(new PixelFormat().withDepthBits(24).withSamples(4));
			Display.setTitle(TITLE);
			// Multisample (Kenar yumuşatma) aktifleştirilir
			GL11.glEnable(GL13.GL_MULTISAMPLE);
		} catch (LWJGLException e) {
			e.printStackTrace();
			System.err.println("Couldn't create display!");
			System.exit(-1);
		}	
		// Çizim alanının boyutları pencereyle aynı yapılır
		GL11.glViewport(0,0, WIDTH, HEIGHT);
		return new DisplayManager();
	}
	
	/**
	 * Private kurucu metot, sadece içeriden (createDisplay metodu ile) çağrılabilir.
	 * Başlangıç zamanını kaydeder.
	 */
	private DisplayManager(){		
		lastFrameTime = getCurrentTime();
	}
	
	/**
	 * Her karede (frame) çağrılarak pencereyi günceller, FPS'yi sınırlar
	 * ve iki kare arasında geçen zamanı (Delta Time) hesaplar.
	 */
	public void update(){
		// FPS sabitleme
		Display.sync(FPS_CAP);
		// Çizilenleri ekrana yansıt (Swap buffers)
		Display.update();
		
		// Geçen süreyi hesapla
		long currentFrameTime = getCurrentTime();
		delta = (currentFrameTime - lastFrameTime)/1000f; // Saniyeye çeviriyoruz
		lastFrameTime = currentFrameTime;
	}
	
	/**
	 * @return İki kare arasında geçen süre (Saniye)
	 */
	public float getFrameTime(){
		return delta;
	}
	
	/**
	 * Pencereyi kapatır ve ayrılan kaynakları temizler.
	 */
	public void closeDisplay(){
		Display.destroy();
	}
	
	/**
	 * @return Sistemin anlık zamanını milisaniye cinsinden döndürür
	 */
	private long getCurrentTime(){
		return Sys.getTime()*1000/Sys.getTimerResolution();
	}
	
}
