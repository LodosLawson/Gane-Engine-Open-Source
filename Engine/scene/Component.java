package scene;

/**
 * Bileşen Tabanlı Mimari (Component-Based Architecture) için temel bileşen sınıfı.
 * GameObject içerisine eklenebilen her bir özellik (Işık, Ses, Can, Hareket) bu sınıftan türetilir.
 */
public abstract class Component {
	
	protected GameObject gameObject;

	public void setGameObject(GameObject gameObject) {
		this.gameObject = gameObject;
	}

	/** Bileşen sahneye ilk eklendiğinde çalışır (Örn: Değişkenleri başlatmak için) */
	public abstract void start();

	/** Her saniye/karede güncellenir (Örn: Hareket işlemleri) */
	public abstract void update(float delta);

}
