package utils;

import org.lwjgl.util.vector.Vector3f;

/**
 * Sinematik kamera rotasinda kullanilan tek bir anahtar kare (keyframe).
 * Kameranin belirli bir anda nerede olmasi ve hangi yonde bakmasi gerektigini tanimlar.
 */
public class CameraKeyframe {

	/** Kameranin dunya konumu */
	public final Vector3f position;
	/** Kameranin asagi/yukari egim acisi (derece) */
	public final float pitch;
	/** Kameranin sol/sag donme acisi (derece) */
	public final float yaw;
	/** Zaman damgasi (saniye) — keyframe'in canlanacagi zaman */
	public final float time;

	/**
	 * @param position Dunya konumu
	 * @param pitch    Dikey egim (pozitif = asagi bakar)
	 * @param yaw      Yatay donus
	 * @param time     Bu noktaya ulasilacak zaman (saniye)
	 */
	public CameraKeyframe(Vector3f position, float pitch, float yaw, float time) {
		this.position = new Vector3f(position);
		this.pitch = pitch;
		this.yaw = yaw;
		this.time = time;
	}

}
