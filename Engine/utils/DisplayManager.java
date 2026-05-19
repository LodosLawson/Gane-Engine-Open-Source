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
			int width = gane.AppSettings.width;
			int height = gane.AppSettings.height;
			boolean fullscreen = gane.AppSettings.fullscreen;
			String title = gane.AppSettings.title;
			String logoPath = gane.AppSettings.logoPath;

			if (fullscreen) {
				Display.setDisplayMode(Display.getDesktopDisplayMode());
				Display.setFullscreen(true);
			} else {
				Display.setDisplayMode(new DisplayMode(width, height));
				Display.setFullscreen(false);
			}
			
			// Depth buffer bit derinliği 24 ve Antialiasing (Multisample) seviyesi 4 olarak ayarlanır
			Display.create(new PixelFormat().withDepthBits(24).withSamples(4));
			Display.setTitle(title);
			
			// Eğer geliştirici özel bir logo/ikon yolu belirttiyse yükle
			if (logoPath != null && !logoPath.isEmpty()) {
				setWindowIcon(logoPath);
			}

			// Multisample (Kenar yumuşatma) aktifleştirilir
			GL11.glEnable(GL13.GL_MULTISAMPLE);
		} catch (LWJGLException e) {
			e.printStackTrace();
			System.err.println("Couldn't create display!");
			System.exit(-1);
		}	
		// Çizim alanının boyutları pencere boyutuyla aynı yapılır
		GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
		return new DisplayManager();
	}

	/**
	 * Geliştiricinin verdiği logo dosyasını dinamik olarak 16x16 ve 32x32 boyutlarına 
	 * ölçeklendirerek pencere ikonu (Window Icon) olarak atar.
	 */
	private static void setWindowIcon(String path) {
		try {
			java.io.InputStream in = new MyFile(path).getInputStream();
			if (in != null) {
				java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(in);
				in.close();
				if (img != null) {
					java.nio.ByteBuffer buf16 = getIconBuffer(img, 16, 16);
					java.nio.ByteBuffer buf32 = getIconBuffer(img, 32, 32);
					Display.setIcon(new java.nio.ByteBuffer[] { buf16, buf32 });
				}
			}
		} catch (Exception e) {
			System.err.println("Pencere logosu yuklenirken hata olustu: " + e.getMessage());
		}
	}

	private static java.nio.ByteBuffer getIconBuffer(java.awt.image.BufferedImage img, int w, int h) {
		java.awt.image.BufferedImage scaledImg = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
		java.awt.Graphics2D g = scaledImg.createGraphics();
		g.drawImage(img, 0, 0, w, h, null);
		g.dispose();

		int[] pixels = new int[w * h];
		scaledImg.getRGB(0, 0, w, h, pixels, 0, w);
		java.nio.ByteBuffer buf = org.lwjgl.BufferUtils.createByteBuffer(w * h * 4);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int pixel = pixels[y * w + x];
				buf.put((byte) ((pixel >> 16) & 0xFF)); // R
				buf.put((byte) ((pixel >> 8) & 0xFF));  // G
				buf.put((byte) (pixel & 0xFF));         // B
				buf.put((byte) ((pixel >> 24) & 0xFF)); // A
			}
		}
		buf.flip();
		return buf;
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
