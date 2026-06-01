package utils;

import org.lwjgl.util.vector.Matrix4f;

public class Frustum {

	private float[][] planes = new float[6][4];

	/**
	 * Güncel View-Projection (Kamera * Projeksiyon) matrisini kullanarak 
	 * kameranın görebildiği alanı (Frustum) hesaplar.
	 * 
	 * @param m Kameranın güncel Projection-View Matrisi
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

		
		// Düzlemleri Normalize Et
		for (int i = 0; i < 6; i++) {
			float length = (float) Math.sqrt(planes[i][0] * planes[i][0] + planes[i][1] * planes[i][1] + planes[i][2] * planes[i][2]);
			if(length > 0) {
				planes[i][0] /= length;
				planes[i][1] /= length;
				planes[i][2] /= length;
				planes[i][3] /= length;
			}
		}
	}

	/**
	 * Verilen bir noktanın kameranın görüş alanında (Frustum) olup olmadığını test eder.
	 * Küre şeklinde (Sphere) bir yaklaşım kullanır.
	 * 
	 * @param x X konumu
	 * @param y Y konumu
	 * @param z Z konumu
	 * @param radius Objeyi saran hayali kürenin yarıçapı
	 * @return Obje ekrandaysa true, değilse false
	 */
	public boolean isPointInside(float x, float y, float z, float radius) {
		for (int i = 0; i < 6; i++) {
			float distance = planes[i][0] * x + planes[i][1] * y + planes[i][2] * z + planes[i][3];
			// Eğer obje tamamen bu düzlemin arkasındaysa (yarıçapı kadar bile görünmüyorsa) dışarıdadır.
			if (distance <= -radius) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Eksenel Hizalanmış Sınırlayıcı Kutunun (AABB) Frustum içinde olup olmadığını test eder.
	 */
	public boolean isBoxInside(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		for (int i = 0; i < 6; i++) {
			float px = planes[i][0] > 0 ? maxX : minX;
			float py = planes[i][1] > 0 ? maxY : minY;
			float pz = planes[i][2] > 0 ? maxZ : minZ;
			
			if (planes[i][0] * px + planes[i][1] * py + planes[i][2] * pz + planes[i][3] < 0) {
				return false;
			}
		}
		return true;
	}
}
