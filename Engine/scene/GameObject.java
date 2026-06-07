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
	private scene.animation.Animator animator;
	private String originalFilePath; // Scene serialization (Save/Load) icin
	
	public String getOriginalFilePath() {
		return originalFilePath;
	}

	/**
	 * Otomatik Model ve Kaplama yükleyen Standart Constructor.
	 * Sadece renk haritası (diffuse) vermek isterseniz roughnessFilePath kısmını null geçebilirsiniz.
	 */
	public GameObject(String objFilePath, String colorFilePath, String roughnessFilePath) {
		super(loadModelSafe(objFilePath), loadSkinSafe(colorFilePath, roughnessFilePath));
		this.originalFilePath = objFilePath;
		if (objFilePath != null && objFilePath.toLowerCase().endsWith(".glb")) {
			this.getModelOffsetRot().set(0, 0, 0);
		}
		initAnimation(getModel());
	}

	/**
	 * Otomatik Model ve Kaplama yükleyen İki Parametreli Constructor.
	 * Sadece model ve ana renk haritası (diffuse) yüklemek için kullanılır.
	 */
	public GameObject(String objFilePath, String colorFilePath) {
		super(loadModelSafe(objFilePath), loadSkinSafe(colorFilePath, null));
		this.originalFilePath = objFilePath;
		if (objFilePath != null && objFilePath.toLowerCase().endsWith(".glb")) {
			this.getModelOffsetRot().set(0, 0, 0);
		}
		initAnimation(getModel());
	}

	// Multi-Mesh desteği için çocuk objeler listesi
	private List<GameObject> multiMeshParts = new ArrayList<>();

	/**
	 * Sadece model dosyasını alıp, eğer içinde gömülü resim (embedded texture) varsa
	 * onu çıkarıp otomatik olarak Skin oluşturan Tek Parametreli Constructor.
	 * Ayrıca Multi-Mesh (Çoklu Parça) dosyalarını destekler.
	 */
	public GameObject(String glbFilePath) {
		super(null, null); // Ana taşıyıcı (Görünmez)
		this.originalFilePath = glbFilePath;
		
		java.util.List<Model> models = modelLoader.loadModels(new utils.MyFile(glbFilePath));
		
		if (models.size() == 1) {
			// Eğer tek parça ise direkt ana objeye yükle
			Model model = models.get(0);
			setModel(model);
			byte[] embeddedTextureData = model.getModelData() != null ? model.getModelData().getEmbeddedTextureData() : null;
			if (embeddedTextureData != null) {
				Texture colorTex = Texture.newTextureFromBuffer(embeddedTextureData).anisotropic().create();
				setSkin(new Skin(colorTex, null));
			} else {
				Texture fallbackTex = Texture.newTexture(new utils.MyFile("res/WoodFloor004.png")).anisotropic().create();
				setSkin(new Skin(fallbackTex, null));
			}
			this.getModelOffsetRot().set(0, 0, 0); // GLB objelerinin yan yatmaması için sıfırlandı
			initAnimation(model);
		} else {
			// Eğer çok parçalı (Multi-Mesh) ise çocuk (child) objeler üret
			for (Model model : models) {
				GameObject child = new GameObject(model, null);
				byte[] embeddedTextureData = model.getModelData() != null ? model.getModelData().getEmbeddedTextureData() : null;
				Skin skin;
				if (embeddedTextureData != null) {
					Texture colorTex = Texture.newTextureFromBuffer(embeddedTextureData).anisotropic().create();
					skin = new Skin(colorTex, null);
				} else {
					Texture fallbackTex = Texture.newTexture(new utils.MyFile("res/WoodFloor004.png")).anisotropic().create();
					skin = new Skin(fallbackTex, null);
				}
				
				skin.setCullBackFaces(!model.getModelData().isDoubleSided());
				skin.setTransparent(model.getModelData().isTransparent());
				skin.setBaseColorFactor(model.getModelData().getBaseColorFactor());
				
				child.setSkin(skin);
				child.getModelOffsetRot().set(0, 0, 0); // GLB objelerinin yan yatmaması için sıfırlandı
				multiMeshParts.add(child);
			}
			// Multi-mesh için ana animasyonu (varsa) ilk parçadan alabiliriz, fakat genelde multi-mesh animasyonları karmaşıktır.
			if (!models.isEmpty()) {
				initAnimation(models.get(0));
			}
		}
	}

	public List<GameObject> getMultiMeshParts() {
		return multiMeshParts;
	}

	public GameObject(Model model, Skin skin) {
		super(model, skin);
		initAnimation(model);
	}

	public scene.animation.Animator getAnimator() {
		return animator;
	}

	private void initAnimation(Model model) {
		if (model != null && model.getModelData() != null) {
			objConverter.ModelData data = model.getModelData();
			if (data.getAnimation() != null && data.getRootJoint() != null) {
				this.animator = new scene.animation.Animator(this, data.getRootJoint(), data.getJointCount());
				this.animator.doAnimation(data.getAnimation());
				this.addComponent(new Component() {
					@Override
					public void start() {}

					@Override
					public void update(float delta) {
						if (animator != null) {
							animator.update(delta);
						}
					}
				});
			}
		}
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

	/** Objeye eklenmis ozel scriptlerin sinif isimlerini dondurur (JSON Kaydi icin) */
	public List<String> getScriptClassNames() {
		List<String> scriptNames = new ArrayList<>();
		for (Component c : components) {
			String name = c.getClass().getName();
			// Anonim siniflari (ornegin GameObject$1 olan Animator) atliyoruz
			if (!name.contains("$") && name.startsWith("scripts.")) {
				scriptNames.add(name);
			}
		}
		return scriptNames;
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
		
		// 1. Multi-mesh parçalarını ana objeye senkronize et
		if (!multiMeshParts.isEmpty()) {
			for (GameObject part : multiMeshParts) {
				part.getPosition().set(this.getPosition());
				part.getRotation().set(this.getRotation());
				part.setScale(this.getScale());
				part.update(delta); // Animasyon veya alt bileşenler için çocuğu da güncelle
			}
		}

		// 2. Eklenen tüm harici bileşenleri (Komponentleri) güncelle
		for (Component c : components) {
			c.update(delta);
		}
		
		// 3. Geliştiricinin kendi yazdığı objeye has mantığı çalıştır
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
