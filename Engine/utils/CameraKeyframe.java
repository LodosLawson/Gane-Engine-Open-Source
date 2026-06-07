package utils;

import org.lwjgl.util.vector.Vector3f;

/**
 * Sinematik kamera rotasında kullanılan tek bir anahtar kare (keyframe) verisi.
 * Kameranın belirli bir zaman diliminde tam olarak nerede (pozisyon) olması ve 
 * nereye bakması (pitch, yaw) gerektiğini tanımlar.
 * 
 * <p>Birden fazla keyframe birleştirilerek spline (eğri) interpolasyonu ile 
 * pürüzsüz kamera hareketleri ve ara sahneler (cutscene) oluşturulabilir.</p>
 * 
 * <p><b>Kullanım Örneği:</b></p>
 * <pre>
 * {@code
 * // 0. saniyede (başlangıçta) (0, 10, 0) konumunda, 10 derece aşağı bakan keyframe
 * CameraKeyframe startFrame = new CameraKeyframe(new Vector3f(0, 10, 0), 10f, 0f, 0f);
 * 
 * // 5. saniyede (50, 20, 50) konumuna gidip 25 derece eğilen ve 90 derece dönen keyframe
 * CameraKeyframe endFrame = new CameraKeyframe(new Vector3f(50, 20, 50), 25f, 90f, 5f);
 * 
 * // Bu iki frame CinematicCamera sınıfına verilerek aradaki hareket otomatik hesaplanır
 * cinematicCamera.addKeyframe(startFrame);
 * cinematicCamera.addKeyframe(endFrame);
 * }
 * </pre>
 */
public class CameraKeyframe {

	/** Kameranın 3D dünyadaki konumu (X, Y, Z kordinatları). */
	public final Vector3f position;
	
	/** Kameranın aşağı/yukarı eğim açısı (Derece cinsinden). Pozitif değerler aşağı bakmayı sağlar. */
	public final float pitch;
	
	/** Kameranın sol/sağ yatay dönme açısı (Derece cinsinden). */
	public final float yaw;
	
	/** Zaman damgası (Saniye cinsinden). Kameranın bu keyframe'in ayarlarına tam olarak ulaşacağı zaman. */
	public final float time;

	/**
	 * Yeni bir kamera anahtar karesi oluşturur.
	 * Position referansının dışarıdan değiştirilmesini önlemek için Vector3f nesnesinin bir kopyası alınır.
	 * 
	 * @param position Kameranın dünya (world) konumu.
	 * @param pitch    Dikey eğim açısı (Pitch). Pozitif ise kamera aşağı, negatif ise yukarı bakar.
	 * @param yaw      Yatay dönüş açısı (Yaw). Kameranın sağa veya sola ne kadar döndüğü.
	 * @param time     Kameranın bu pozisyon ve açı değerlerine ulaşması gereken zaman (Saniye cinsinden).
	 */
	public CameraKeyframe(Vector3f position, float pitch, float yaw, float time) {
		this.position = new Vector3f(position);
		this.pitch = pitch;
		this.yaw = yaw;
		this.time = time;
	}

}
