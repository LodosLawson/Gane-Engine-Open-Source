package utils;

import org.lwjgl.util.vector.Matrix4f;

/**
 * Kameranın görüş alanını (View Frustum) hesaplamak ve yönetmek için kullanılan sınıf.
 * 3D dünyadaki objelerin kameranın görüş alanı (ekran) içinde kalıp kalmadığını test eder.
 * Görünmeyen objeleri (Culling) render işleminden çıkartarak performansı artırır (Frustum Culling).
 * 
 * <p><b>Kullanım Örneği:</b></p>
 * <pre>
 * {@code
 * Frustum frustum = new Frustum();
 * 
 * // Kamera ve projeksiyon matrisleri çarpılarak View-Projection matrisi elde edilir
 * Matrix4f viewProjectionMatrix = Matrix4f.mul(projectionMatrix, viewMatrix, null);
 * 
 * // Frustum her karede kameranın yeni konumuna göre güncellenir
 * frustum.update(viewProjectionMatrix);
 * 
 * // Bir objenin ekranda görünüp görünmediğini test et (Yarıçapı 5 birim olan küre)
 * boolean inSight = frustum.isPointInside(entity.getX(), entity.getY(), entity.getZ(), 5f);
 * 
 * if (inSight) {
 *     // Obje kameranın önünde ve ekranda, render et!
 *     renderer.processEntity(entity);
 * }
 * }
 * </pre>
 */
public class Frustum {

	/** Frustum'u oluşturan 6 düzlem (Sol, Sağ, Alt, Üst, Yakın, Uzak). Her düzlem için 4 değer (A, B, C, D) saklanır. */
	private float[][] planes = new float[6][4];

	/**
	 * Güncel View-Projection (Kamera * Projeksiyon) matrisini kullanarak 
	 * kameranın görebildiği alanı (Frustum) hesaplar ve içindeki düzlemleri günceller.
	 * Her render döngüsünde (kamera hareket ettiğinde) çağrılmalıdır.
	 * 
	 * @param m Kameranın güncel View-Projection Matrisi (Projeksiyon Matrisi x Görünüm Matrisi).
	 */
	public void update(Matrix4f m) {
		// Sol Düzlem (Left)
		planes[0][0] = m.m03 + m.m00;
		planes[0][1] = m.m13 + m.m10;
		planes[0][2] = m.m23 + m.m20;
		planes[0][3] = m.m33 + m.m30;
		
		// Sağ Düzlem (Right)
		planes[1][0] = m.m03 - m.m00;
		planes[1][1] = m.m13 - m.m10;
		planes[1][2] = m.m23 - m.m20;
		planes[1][3] = m.m33 - m.m30;
		
		// Alt Düzlem (Bottom)
		planes[2][0] = m.m03 + m.m01;
		planes[2][1] = m.m13 + m.m11;
		planes[2][2] = m.m23 + m.m21;
		planes[2][3] = m.m33 + m.m31;
		
		// Üst Düzlem (Top)
		planes[3][0] = m.m03 - m.m01;
		planes[3][1] = m.m13 - m.m11;
		planes[3][2] = m.m23 - m.m21;
		planes[3][3] = m.m33 - m.m31;
		
		// Yakın Düzlem (Near)
		planes[4][0] = m.m03 + m.m02;
		planes[4][1] = m.m13 + m.m12;
		planes[4][2] = m.m23 + m.m22;
		planes[4][3] = m.m33 + m.m32;
		
		// Uzak Düzlem (Far)
		planes[5][0] = m.m03 - m.m02;
		planes[5][1] = m.m13 - m.m12;
		planes[5][2] = m.m23 - m.m22;
		planes[5][3] = m.m33 - m.m32;

		// Düzlemlerin normal vektörlerini hesaplayarak düzlemi normalize et (Birim vektör haline getir)
		for (int i = 0; i < 6; i++) {
			float length = (float) Math.sqrt(planes[i][0] * planes[i][0] + planes[i][1] * planes[i][1] + planes[i][2] * planes[i][2]);
			// Sıfıra bölme hatasını (Divide by zero) engellemek için güvenlik kontrolü
			if(length > 0.0001f) {
				planes[i][0] /= length;
				planes[i][1] /= length;
				planes[i][2] /= length;
				planes[i][3] /= length;
			}
		}
	}

	/**
	 * Verilen bir 3D noktanın kameranın görüş alanında (Frustum) olup olmadığını test eder.
	 * Küre şeklinde (Bounding Sphere) bir yaklaşım kullanır. Objenin merkez noktası ve 
	 * objeyi çevreleyen sanal bir kürenin yarıçapı verilerek test yapılır.
	 * 
	 * @param x Objenin merkez X konumu
	 * @param y Objenin merkez Y konumu
	 * @param z Objenin merkez Z konumu
	 * @param radius Objeyi saran hayali kürenin yarıçapı
	 * @return Obje tamamen veya kısmen ekrandaysa true, tamamen ekran dışındaysa false.
	 */
	public boolean isPointInside(float x, float y, float z, float radius) {
		for (int i = 0; i < 6; i++) {
			float distance = planes[i][0] * x + planes[i][1] * y + planes[i][2] * z + planes[i][3];
			// Eğer obje tamamen bu düzlemin arkasındaysa (merkeze uzaklığı yarıçaptan daha gerideyse) dışarıdadır.
			if (distance <= -radius) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Eksenel Hizalanmış Sınırlayıcı Kutunun (AABB - Axis-Aligned Bounding Box) 
	 * görüş alanının (Frustum) içinde olup olmadığını test eder.
	 * Özellikle karmaşık şekilli objeler veya terrain (arazi) parçaları için kullanılır.
	 * 
	 * @param minX Kutunun minimum X kordinatı
	 * @param minY Kutunun minimum Y kordinatı
	 * @param minZ Kutunun minimum Z kordinatı
	 * @param maxX Kutunun maksimum X kordinatı
	 * @param maxY Kutunun maksimum Y kordinatı
	 * @param maxZ Kutunun maksimum Z kordinatı
	 * @return Kutu görüş alanının tamamen veya kısmen içindeyse true, dışındaysa false.
	 */
	public boolean isBoxInside(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		for (int i = 0; i < 6; i++) {
			// Düzlemin normal yönüne göre AABB'nin pozitif ucunu (p-vertex) buluyoruz
			float px = planes[i][0] > 0 ? maxX : minX;
			float py = planes[i][1] > 0 ? maxY : minY;
			float pz = planes[i][2] > 0 ? maxZ : minZ;
			
			// Eğer p-vertex düzlemin arkasında kalıyorsa, bütün kutu düzlemin arkasındadır
			if (planes[i][0] * px + planes[i][1] * py + planes[i][2] * pz + planes[i][3] < 0) {
				return false;
			}
		}
		return true;
	}
}

