package lensFlare;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import utils.ICamera;

/**
 * Lens Flare (Mercek Parlaması) efektini yöneten sınıf.
 * Güneşin ekran üzerindeki konumunu hesaplar, ekrana göre görünürlüğünü belirler 
 * ve parlamaların (flare) konumlarını düzenler.
 */
public class FlareManager {

	// Ekranın tam orta noktası. Vektör hesabı için kullanılıyor.
	private static final Vector2f CENTER_SCREEN = new Vector2f(0f, 0f);//center changed

	// Mercek parlamasını oluşturacak dokuların listesi
	private final FlareTexture[] flareTextures;
	// Ardışık parlama dokuları arasındaki mesafe
	private final float spacing;

	// Parlama dokularını ekrana çizen render nesnesi
	private FlareRenderer renderer;

	/**
	 * FlareManager'ı ilklendirir.
	 * 
	 * @param spacing Dokular arasındaki boşluk miktarı
	 * @param textures Mercek parlamasını oluşturan doku nesneleri
	 */
	public FlareManager(float spacing, FlareTexture... textures) {
		this.spacing = spacing;
		this.flareTextures = textures;
		this.renderer = new FlareRenderer();
	}

	/**
	 * Her karede çağrılır. Mercek parlamalarının mantığını çalıştırır ve çizer.
	 * 
	 * @param camera Oyuncunun kamerası
	 * @param sunWorldPos Güneşin 3B dünya koordinatlarındaki pozisyonu
	 */
	public void render(ICamera camera, Vector3f sunWorldPos) {
		// Güneşin dünya koordinatını, kameranın baktığı ekran koordinatına çevirir
		Vector2f sunCoords = convertToScreenSpace(sunWorldPos, camera.getViewMatrix(), camera.getProjectionMatrix());
		// Eğer güneş kameranın arkasında kalıyorsa hiçbir işlem yapma
		if(sunCoords == null){
			return;
		}
		// Güneş ile ekranın merkezi arasındaki yön ve mesafe vektörü
		Vector2f sunToCenter = Vector2f.sub(CENTER_SCREEN, sunCoords, null);
		// Güneş ekrandan ne kadar uzaklaşırsa parlaklık o kadar düşer (solma efekti)
		float brightness = 1 - (sunToCenter.length() / 1.4f);//number doubled
		if(brightness > 0){
			// Parlamaların ekran pozisyonlarını hesapla
			calcFlarePositions(sunToCenter, sunCoords);
			// FlareRenderer'a çimdirme komutu ver
			renderer.render(sunCoords, flareTextures, brightness);
		}
	}
	
	/**
	 * Her bir parlama dokusunun ekrandaki konumunu hesaplar ve ayarlar.
	 * 
	 * @param sunToCenter Güneşten ekranın merkezine giden vektör
	 * @param sunCoords Güneşin ekran koordinatları
	 */
	private void calcFlarePositions(Vector2f sunToCenter, Vector2f sunCoords){
		for(int i=0;i<flareTextures.length;i++){
			Vector2f direction = new Vector2f(sunToCenter);
			// Her dokuyu farklı bir mesafeye yerleştirmek için direction vektörünü ölçekle
			direction.scale(i * spacing);
			// Güneşin konumuna, ölçeklenmiş yönü ekleyerek dokunun yerini bul
			Vector2f flarePos = Vector2f.add(sunCoords, direction, null);
			flareTextures[i].setScreenPos(flarePos);
		}
	}

	/**
	 * 3B dünya koordinatlarını 2B ekran uzayına (screen space) dönüştürür.
	 * 
	 * @param worldPos 3B dünyadaki nokta (örn: güneşin konumu)
	 * @param viewMat Kameranın görünüm matrisi
	 * @param projectionMat Kameranın projeksiyon matrisi
	 * @return Ekran üzerindeki (x,y) konumu; eğer obje kameranın arkasındaysa null döner.
	 */
	private Vector2f convertToScreenSpace(Vector3f worldPos, Matrix4f viewMat, Matrix4f projectionMat) {
		Vector4f coords = new Vector4f(worldPos.x, worldPos.y, worldPos.z, 1f);
		// Önce kamera alanına (view space) dönüştür
		Matrix4f.transform(viewMat, coords, coords);
		// Sonra kırpma alanına (clip space) dönüştür
		Matrix4f.transform(projectionMat, coords, coords);
		// w <= 0 demek kamera nesnenin önünde değil, arkasında veya tam üstünde demektir
		if (coords.w <= 0) {
			return null;
		}
		//no need for conversion below
		// Homojen koordinatlara (Perspective Divide) bölerek ekran alanına (NDC) çevir
		return new Vector2f(coords.x / coords.w, coords.y / coords.w);
	}

	/**
	 * Oyun kapatılırken bellekten temizlenmesi gereken verileri serbest bırakır.
	 */
	public void cleanUp() {
		renderer.cleanUp();
	}

}
