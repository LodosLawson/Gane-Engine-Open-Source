package scene;

import java.util.ArrayList;
import java.util.List;
import loaders.ModelLoader;
import utils.MyFile;

/**
 * Blender'dan "OBJ Sequence" (OBJ Dizisi) olarak ihraç edilen vertex bazlı animasyonları
 * oynatmak için geliştirilmiş modern anahtar kare (keyframe) animasyon bileşeni.
 * Klasik retro 3D oyunlar (Quake vb.) gibi kare kare modelleri değiştirerek animasyon oynatır.
 */
public class KeyframeAnimationComponent extends Component {

	private final List<Model> frames = new ArrayList<>();
	private float timeBetweenFrames = 0.1f; // Her karenin ekranda kalma süresi (saniye)
	private float elapsedTime = 0f;
	private int currentFrameIndex = 0;
	private boolean loop = true;
	private boolean playing = true;

	private static final ModelLoader modelLoader = new ModelLoader();

	public KeyframeAnimationComponent() {}

	/**
	 * Animasyon karelerini yükler.
	 * Örneğin: "res/anim/yurume_", 0, 10, ".obj" -> yurume_0.obj'den yurume_10.obj'ye kadar yükler.
	 */
	public void loadFrames(String prefix, int startFrame, int endFrame, String suffix) {
		for (int i = startFrame; i <= endFrame; i++) {
			String path = prefix + i + suffix;
			try {
				Model model = modelLoader.loadModel(new MyFile(path));
				frames.add(model);
			} catch (Exception e) {
				System.err.println("Animasyon karesi yuklenemedi: " + path);
			}
		}
	}

	/** Saniyedeki kare sayısını (FPS) ayarlar */
	public void setFPS(float fps) {
		this.timeBetweenFrames = 1.0f / fps;
	}

	public void setLoop(boolean loop) {
		this.loop = loop;
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
		if (!playing || frames.isEmpty() || gameObject == null) return;

		elapsedTime += delta;
		if (elapsedTime >= timeBetweenFrames) {
			elapsedTime = 0f;
			currentFrameIndex++;
			
			if (currentFrameIndex >= frames.size()) {
				if (loop) {
					currentFrameIndex = 0;
				} else {
					currentFrameIndex = frames.size() - 1;
					playing = false;
				}
			}
			updateEntityModel();
		}
	}

	private void updateEntityModel() {
		if (gameObject != null && !frames.isEmpty()) {
			gameObject.setModel(frames.get(currentFrameIndex));
		}
	}
}
