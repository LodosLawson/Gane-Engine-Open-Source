package scene;

import java.util.ArrayList;
import java.util.List;

import loaders.ModelLoader;
import textures.Texture;
import utils.MyFile;

/**
 * Modern Oyun Motorlarındaki (Unity/Unreal) "Prefab" veya "Actor" benzeri modüler yapı.
 * Geliştirici kendi objelerini (Örn: Oyuncu, Ağaç, Düşman) bu sınıftan miras alarak türetir.
 * Model ve Kaplama yükleme işlemleri burada arka planda gizlice halledilir.
 */
public class GameObject extends Entity {
	
	private List<Component> components = new ArrayList<>();
	private static ModelLoader modelLoader = new ModelLoader();
	private boolean started = false;

	/**
	 * Otomatik Model ve Kaplama yükleyen Standart Constructor.
	 * Sadece renk haritası (diffuse) vermek isterseniz roughnessFilePath kısmını null geçebilirsiniz.
	 */
	public GameObject(String objFilePath, String colorFilePath, String roughnessFilePath) {
		super(loadModelSafe(objFilePath), loadSkinSafe(colorFilePath, roughnessFilePath));
	}

	public GameObject(Model model, Skin skin) {
		super(model, skin);
	}
	
	private static Model loadModelSafe(String objPath) {
		return modelLoader.loadModel(new MyFile(objPath));
	}
	
	private static Skin loadSkinSafe(String colorPath, String roughnessPath) {
		Texture colorTex = Texture.newTexture(new MyFile(colorPath)).anisotropic().create();
		if (roughnessPath != null && !roughnessPath.isEmpty()) {
			Texture roughTex = Texture.newTexture(new MyFile(roughnessPath)).anisotropic().create();
			return new Skin(colorTex, roughTex);
		}
		return new Skin(colorTex, null);
	}

	/** Objeye yeni bir bileşen (Işık, Ses, Fizik) ekler */
	public void addComponent(Component component) {
		component.setGameObject(this);
		components.add(component);
		if (started) {
			component.start();
		}
	}
	
	/** Obje üzerindeki belirli bir bileşeni getirir */
	public <T extends Component> T getComponent(Class<T> type) {
		for (Component c : components) {
			if (type.isAssignableFrom(c.getClass())) {
				return type.cast(c);
			}
		}
		return null;
	}

	/** Oyun döngüsü başladığında objenin sahneye girdiği ilk an çalışır */
	public void start() {
		started = true;
		for (Component c : components) {
			c.start();
		}
	}

	@Override
	public void update(float delta) {
		if (!started) {
			start();
		}
		
		// 1. Eklenen tüm harici bileşenleri (Komponentleri) güncelle
		for (Component c : components) {
			c.update(delta);
		}
		
		// 2. Geliştiricinin kendi yazdığı objeye has mantığı çalıştır
		onUpdate(delta);
	}
	
	/**
	 * Geliştirici bu objeye özel olan hareket, ses, saldırı vb. mantıkları buraya yazacak.
	 * (Override edilerek kullanılır)
	 */
	protected void onUpdate(float delta) {
		// Alt sınıflar (Örn: Oyuncu, AhsapZemin) burayı dolduracak.
	}
}
