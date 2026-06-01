package objects;

import scene.GameObject;
import scene.Model;
import scene.Skin;
import loaders.ModelLoader;
import textures.Texture;
import utils.MyFile;

public class Grass3D extends GameObject {

	private static Model cachedModel = null;
	private static Skin cachedSkin = null;

	public Grass3D() {
		super(getGrassModel(), getGrassSkin());
		
		// Çimen transparanlığı içerir (alpha cut-out)
		this.getSkin().setTransparent(true);
		
		// Çimen için sahte ışıklandırma (Normaller yukarı bakar, arazi ışığıyla uyumlu olur ve gün batımında kararmalar düzelir)
		this.getSkin().setUseFakeLighting(true);
	}

	public static Model getGrassModel() {
		if (cachedModel == null) {
			cachedModel = new ModelLoader().loadModel(new MyFile("res/DEFAULT_GRASS_3D/textures grass bush/GRASS3D.obj"));
		}
		return cachedModel;
	}

	public static Skin getGrassSkin() {
		if (cachedSkin == null) {
			Texture tex = Texture.newTexture(new MyFile("res/DEFAULT_GRASS_3D/textures grass bush/diffuse_grass_bush.jpg")).anisotropic().create();
			cachedSkin = new Skin(tex, null);
			cachedSkin.setTransparent(true);
			cachedSkin.setUseFakeLighting(true);
			cachedSkin.setCullBackFaces(false); // Çimenlerin iki yönden de görünmesi gerekir
		}
		return cachedSkin;
	}

	@Override
	protected void onUpdate(float delta) {
		// Herhangi bir özel animasyon yoksa boş kalabilir.
	}
}
