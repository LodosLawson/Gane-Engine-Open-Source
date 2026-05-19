package scene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import loaders.ModelLoader;
import utils.MyFile;

/**
 * Blender'dan "OBJ Sequence" olarak ihraç edilen vertex bazlı animasyonları
 * durum tabanlı (State-Machine) yöneten gelişmiş anahtar kare animasyon bileşeni.
 * Karakterlerin veya objelerin "idle", "attack", "interact" gibi kliplerini
 * kolayca tanımlayıp tetiklemek için kullanılır.
 */
public class KeyframeAnimationComponent extends Component {

	/** Tek bir animasyon klibini (örneğin yürüme veya saldırma) temsil eden sınıf */
	public static class AnimationClip {
		public String name;
		public List<Model> frames = new ArrayList<>();
		public float timeBetweenFrames = 0.1f;
		public boolean loop = true;

		public AnimationClip(String name, float fps, boolean loop) {
			this.name = name;
			this.timeBetweenFrames = 1.0f / fps;
			this.loop = loop;
		}
	}

	private final Map<String, AnimationClip> clips = new HashMap<>();
	private AnimationClip currentClip = null;
	private AnimationClip defaultClip = null;

	private float elapsedTime = 0f;
	private int currentFrameIndex = 0;
	private boolean playing = true;

	private static final ModelLoader modelLoader = new ModelLoader();

	public KeyframeAnimationComponent() {}

	/**
	 * Yeni bir durum/klip animasyonu yükler ve kaydeder.
	 * Örneğin: registerClip("attack", "res/animt/Başlıksızz", 1, 60, ".obj", 30f, false);
	 */
	public void registerClip(String name, String prefix, int startFrame, int endFrame, String suffix, float fps, boolean loop) {
		AnimationClip clip = new AnimationClip(name, fps, loop);
		
		for (int i = startFrame; i <= endFrame; i++) {
			String paddedNumber = String.format("%04d", i);
			String path = prefix + paddedNumber + suffix;
			
			utils.MyFile myFile = new MyFile(path);
			try {
				Model model = modelLoader.loadModel(myFile);
				clip.frames.add(model);
			} catch (Exception e) {
				// Sıfır dolgusuz olarak dene
				String fallbackPath = prefix + i + suffix;
				try {
					Model model = modelLoader.loadModel(new MyFile(fallbackPath));
					clip.frames.add(model);
				} catch (Exception ex) {
					System.err.println("[" + name + "] Animasyon karesi yuklenemedi: " + path);
				}
			}
		}

		clips.put(name, clip);
		
		// İlk yüklenen klibi varsayılan olarak seç
		if (defaultClip == null) {
			defaultClip = clip;
			currentClip = clip;
		}
	}

	/** Varsayılan (default/idle) animasyon klibini ayarlar */
	public void setDefaultClip(String name) {
		if (clips.containsKey(name)) {
			this.defaultClip = clips.get(name);
			if (currentClip == null) {
				currentClip = defaultClip;
			}
		}
	}

	/** İsmi verilen animasyonu oynatır */
	public void playClip(String name) {
		if (!clips.containsKey(name)) {
			System.err.println("Animasyon klibi bulunamadi: " + name);
			return;
		}
		
		AnimationClip newClip = clips.get(name);
		if (currentClip != newClip) {
			currentClip = newClip;
			currentFrameIndex = 0;
			elapsedTime = 0f;
		}
		playing = true;
		updateEntityModel();
	}

	public void play() {
		this.playing = true;
	}

	public void pause() {
		this.playing = false;
	}

	public void stop() {
		this.playing = false;
		this.currentFrameIndex = 0;
		updateEntityModel();
	}

	@Override
	public void start() {
		updateEntityModel();
	}

	@Override
	public void update(float delta) {
		if (!playing || currentClip == null || currentClip.frames.isEmpty() || gameObject == null) return;

		elapsedTime += delta;
		if (elapsedTime >= currentClip.timeBetweenFrames) {
			elapsedTime = 0f;
			currentFrameIndex++;
			
			if (currentFrameIndex >= currentClip.frames.size()) {
				if (currentClip.loop) {
					currentFrameIndex = 0;
				} else {
					// Eğer döngü kapalıysa (örneğin attack bittiyse) varsayılan animasyona (idle/default) geri dön!
					if (defaultClip != null && currentClip != defaultClip) {
						currentClip = defaultClip;
						currentFrameIndex = 0;
					} else {
						currentFrameIndex = currentClip.frames.size() - 1;
						playing = false;
					}
				}
			}
			updateEntityModel();
		}
	}

	private void updateEntityModel() {
		if (gameObject != null && currentClip != null && !currentClip.frames.isEmpty()) {
			gameObject.setModel(currentClip.frames.get(currentFrameIndex));
		}
	}

	// --- Eski Geriye Dönük Uyumluluk Metotları (Geliştiriciyi kırmamak için) ---
	@Deprecated
	public void loadFrames(String prefix, int startFrame, int endFrame, String suffix) {
		registerClip("default", prefix, startFrame, endFrame, suffix, 24f, true);
		setDefaultClip("default");
		playClip("default");
	}

	@Deprecated
	public void setFPS(float fps) {
		AnimationClip clip = clips.get("default");
		if (clip != null) {
			clip.timeBetweenFrames = 1.0f / fps;
		}
	}

	@Deprecated
	public void setLoop(boolean loop) {
		AnimationClip clip = clips.get("default");
		if (clip != null) {
			clip.loop = loop;
		}
	}
}
