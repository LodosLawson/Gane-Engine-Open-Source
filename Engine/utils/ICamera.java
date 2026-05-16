package utils;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Oyun içerisindeki tüm kamera türlerinin uygulaması gereken temel arayüz (Interface).
 * Kameranın konumunu, görünüm (View) ve yansıtma (Projection) matrislerini sağlar.
 */
public interface ICamera {
	
	/** @return Kameranın 3 boyutlu uzaydaki pozisyonu */
	public Vector3f getPosition();
	
	/** @return Kameranın bakış açısını tanımlayan görünüm (View) matrisi */
	public Matrix4f getViewMatrix();
	
	/**
	 * Kamerayı belirtilen bir yüksekliğe (genellikle su seviyesi) göre dikeyde yansıtır (Ters çevirir).
	 * Suyun altındaki yansımaları çizerken kullanılır.
	 * 
	 * @param height Yansıtma ekseni yüksekliği
	 */
	public void reflect(float height);
	
	/** @return Kameranın projeksiyon (perspektif) matrisi */
	public Matrix4f getProjectionMatrix();
	
	/** @return Projeksiyon ve Görünüm matrislerinin çarpılmış (Projection * View) hali */
	public Matrix4f getProjectionViewMatrix();

}
