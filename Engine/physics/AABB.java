package physics;

import org.lwjgl.util.vector.Vector3f;

/**
 * Eksenlere Hizalanmış Sınır Kutusu (Axis-Aligned Bounding Box - AABB).
 * Bir objenin çevresini saran, dönüş (rotation) içermeyen dikdörtgen/prizma şeklinde çarpışma alanıdır.
 * Hem 2D hem 3D çarpışmalar için kullanılabilir.
 */
public class AABB implements Collider {

	// Objenin merkezinden minimum köşeye olan uzaklık (Sol-Alt-Arka)
	private Vector3f minOffset;
	// Objenin merkezinden maksimum köşeye olan uzaklık (Sağ-Üst-Ön)
	private Vector3f maxOffset;

	/**
	 * Yeni bir AABB oluşturur.
	 * 
	 * @param minOffset Merkez noktasına göre x, y, z için minimum sınırlar
	 * @param maxOffset Merkez noktasına göre x, y, z için maksimum sınırlar
	 */
	public AABB(Vector3f minOffset, Vector3f maxOffset) {
		this.minOffset = minOffset;
		this.maxOffset = maxOffset;
	}

	public Vector3f getMinOffset() {
		return minOffset;
	}

	public Vector3f getMaxOffset() {
		return maxOffset;
	}

	@Override
	public boolean intersects(Collider other, Vector3f thisPos, Vector3f otherPos) {
		if (other instanceof AABB) {
			AABB b = (AABB) other;
			
			// Bu kutunun dünya koordinatları
			float thisMinX = thisPos.x + this.minOffset.x;
			float thisMaxX = thisPos.x + this.maxOffset.x;
			float thisMinY = thisPos.y + this.minOffset.y;
			float thisMaxY = thisPos.y + this.maxOffset.y;
			float thisMinZ = thisPos.z + this.minOffset.z;
			float thisMaxZ = thisPos.z + this.maxOffset.z;

			// Diğer kutunun dünya koordinatları
			float otherMinX = otherPos.x + b.minOffset.x;
			float otherMaxX = otherPos.x + b.maxOffset.x;
			float otherMinY = otherPos.y + b.minOffset.y;
			float otherMaxY = otherPos.y + b.maxOffset.y;
			float otherMinZ = otherPos.z + b.minOffset.z;
			float otherMaxZ = otherPos.z + b.maxOffset.z;

			// AABB Kesişim Mantığı: Eğer tüm eksenlerde örtüşme (overlap) varsa çarpışma vardır.
			boolean intersectX = thisMinX <= otherMaxX && thisMaxX >= otherMinX;
			boolean intersectY = thisMinY <= otherMaxY && thisMaxY >= otherMinY;
			boolean intersectZ = thisMinZ <= otherMaxZ && thisMaxZ >= otherMinZ;

			return intersectX && intersectY && intersectZ;
		}
		
		// Farklı tip collider'lar (örn. Küre) eklendiğinde burada ele alınabilir.
		return false;
	}
}
