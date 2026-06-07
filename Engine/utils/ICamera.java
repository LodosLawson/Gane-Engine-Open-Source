package utils;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Oyun içerisindeki tüm kamera türlerinin (Serbest Kamera, Oyuncu Kamerası, Sinematik Kamera vb.) 
 * uygulaması gereken temel arayüz (Interface).
 * Kameranın konumunu, görünüm (View) ve yansıtma (Projection) matrislerini sağlar.
 * 
 * <p><b>Kullanım Örneği:</b></p>
 * <pre>
 * {@code
 * // ICamera arayüzünü uygulayan herhangi bir kamerayı al
 * ICamera camera = scene.getCamera();
 * 
 * // Shadera (Gölgelendiriciye) kameranın view matrisini yükle
 * shader.loadViewMatrix(camera.getViewMatrix());
 * 
 * // Kameranın pozisyonuna göre sesi ayarla
 * audioManager.setListenerData(camera.getPosition());
 * }
 * </pre>
 */
public interface ICamera {
	
	/**
	 * Kameranın 3D uzaydaki mevcut (X, Y, Z) koordinatlarını döndürür.
	 * 
	 * @return Kameranın 3 boyutlu uzaydaki pozisyon vektörü.
	 */
	public Vector3f getPosition();
	
	/**
	 * Kameranın bakış açısını tanımlayan görünüm (View) matrisini döndürür.
	 * Bu matris, dünyanın kameranın konumuna göre ters yönde kaydırılmasını ve döndürülmesini sağlar.
	 * 
	 * @return Görünüm (View) matrisi.
	 */
	public Matrix4f getViewMatrix();
	
	/**
	 * Kamerayı belirtilen bir yüksekliğe (genellikle su seviyesine) göre dikeyde yansıtır (Ters çevirir).
	 * Su gibi yüzeylerdeki yansımaları çizerken, kamerayı suyun altına alıp yukarı bakmasını sağlamak için kullanılır.
	 * 
	 * @param height Yansıtma işleminin yapılacağı eksen (Y yüksekliği, örn: suyun yüksekliği).
	 */
	public void reflect(float height);
	
	/**
	 * Kameranın projeksiyon (perspektif) matrisini döndürür.
	 * 3 boyutlu dünyanın 2 boyutlu ekrana nasıl izdüşüm yapılacağını tanımlar.
	 * 
	 * @return Projeksiyon (Projection) matrisi.
	 */
	public Matrix4f getProjectionMatrix();
	
	/**
	 * Projeksiyon ve Görünüm matrislerinin çarpılmış (Projection * View) halini döndürür.
	 * Frustum Culling gibi hesaplamalarda sıklıkla kullanılır.
	 * 
	 * @return Görünüm-Projeksiyon matrisi.
	 */
	public Matrix4f getProjectionViewMatrix();
	
	/**
	 * Kameranın X ekseni etrafında aşağı/yukarı dönme açısını döndürür.
	 * 
	 * @return Dikey eğim açısı (Pitch) derece cinsinden.
	 */
	public float getPitch();
	
	/**
	 * Kameranın Y ekseni etrafında sağa/sola dönme açısını döndürür.
	 * 
	 * @return Yatay dönüş açısı (Yaw) derece cinsinden.
	 */
	public float getYaw();

}
