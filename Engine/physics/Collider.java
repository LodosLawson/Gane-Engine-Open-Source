package physics;

import org.lwjgl.util.vector.Vector3f;

/**
 * Fiziksel çarpışma sınırlarını belirlemek için kullanılan temel arayüz.
 * AABB (Kutu) veya Küre (Sphere) gibi farklı çarpışma şekilleri bu arayüzü uygular.
 */
public interface Collider {

	/**
	 * Bu çarpıştırıcının başka bir çarpıştırıcı ile kesişip kesişmediğini kontrol eder.
	 * 
	 * @param other Çarpışma testi yapılacak diğer collider
	 * @param thisPos Bu objenin uzaydaki konumu
	 * @param otherPos Diğer objenin uzaydaki konumu
	 * @return Çarpışma varsa true, yoksa false
	 */
	boolean intersects(Collider other, Vector3f thisPos, Vector3f otherPos);

}
