package scene;

import extra.Camera.CameraMode;
import org.lwjgl.util.vector.Vector3f;

/**
 * Sahneye eklenebilen ve modlari secilebilen bir Kamera objesi.
 */
public class CameraEntity extends Entity {
	
	private CameraMode mode = CameraMode.FIRST_PERSON;
	
	public CameraEntity() {
		// Kameranin default baslangic pozisyonu
		getPosition().set(0, 10, 0);
	}

	public CameraMode getMode() {
		return mode;
	}

	public void setMode(CameraMode mode) {
		this.mode = mode;
	}
}
