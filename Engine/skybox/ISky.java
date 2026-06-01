package skybox;

import org.lwjgl.util.vector.Vector3f;
import utils.ICamera;

/**
 * Gane Engine içerisindeki tüm gökyüzü (Sky) sistemleri için ortak arayüz.
 * İster resim tabanlı (Skybox) ister algoritmik (AtmosphereSky) olsun,
 * tüm gökyüzü sınıfları bu arayüzü uygulamalıdır.
 *
 * Polimorfik render: MasterRenderer instanceof olmadan doğrudan çağırabilir.
 */
public interface ISky {

	/**
	 * Gökyüzünü sahnede çizer.
	 * Her ISky implementasyonu kendi render mantığını buraya sığdırmalıdır.
	 *
	 * @param camera    Aktif kamera
	 * @param lightDir  Güneş/ana ışık yönü
	 */
	void render(ICamera camera, Vector3f lightDir);

	/**
	 * Gökyüzünün silinmesi/temizlenmesi gerektiğinde çağrılır.
	 */
	void cleanUp();

}
