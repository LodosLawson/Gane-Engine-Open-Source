package objects;

import loaders.ModelLoader;
import scene.GameObject;
import scene.Model;
import scene.Skin;
import textures.Texture;
import utils.MyFile;

public class Tree3D extends GameObject {

	private static Model cachedModel = null;
	private static Model cachedModelLod1 = null;
	private static Skin cachedSkin = null;

	public Tree3D() {
		super(getTreeModel(), getTreeSkin());
		this.getSkin().setTransparent(true);
		this.getSkin().setUseFakeLighting(true);
	}

	public static Model getTreeModel() {
		return getTreeModelLod1();
	}

	public static Model getTreeModelLod1() {
		if (cachedModelLod1 == null) {
			// LOD1: Lower-poly tree model (3.7MB)
			cachedModelLod1 = new ModelLoader().loadModel(new MyFile("res/DEFAULT_TREE_MODEL_S/TREE.obj"));
		}
		return cachedModelLod1;
	}

	public static Skin getTreeSkin() {
		if (cachedSkin == null) {
			Texture tex = Texture.newTexture(new MyFile("res/DEFAULT_TREE_MODEL_S/Yeni Proje.png")).anisotropic()
					.create();
			cachedSkin = new Skin(tex, null);
			// Trees usually have alpha cut-out leaves
			cachedSkin.setTransparent(true);
			// Fake lighting helps tree canopies look softer like grass
			cachedSkin.setUseFakeLighting(true);
		}
		return cachedSkin;
	}

	@Override
	protected void onUpdate(float delta) {
		// Herhangi bir özel animasyon yoksa boş kalabilir.
	}
}
