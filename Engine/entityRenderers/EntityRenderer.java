package entityRenderers;

import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import openglObjects.Vao;
import scene.Entity;
import scene.Skin;
import utils.ICamera;
import utils.OpenGlUtils;

/**
 * Bu sınıf sahnedeki nesneleri (entity) ekrana çizdirmek (render) için kullanılır.
 * Oyun içindeki 3D modellerin görüntülenmesinden sorumludur.
 */
public class EntityRenderer {
	
	/**
	 * Nesnelerin çiziminde kullanılan shader (gölgelendirici) programı.
	 * Gölgelendirme, renk ve ışık hesaplamaları için gereklidir.
	 */
	private EntityShader shader;

	/**
	 * EntityRenderer sınıfının yapıcı (constructor) metodu.
	 * Shader objesini oluşturur ve hazırlar.
	 */
	public EntityRenderer() {
		this.shader = new EntityShader();
	}

	/**
	 * Listede bulunan tüm nesneleri (entities) ekrana çizer.
	 * Neden: Oyun döngüsünde her karede (frame) nesnelerin görünür olmasını sağlamak için çağrılır.
	 * @param entities Çizilecek nesnelerin listesi.
	 * @param camera Oyuncunun kamerası, bakış açısı.
	 * @param lightDir Güneş ışığının veya ana ışığın yönü.
	 * @param clipPlane Kırpma düzlemi, su yansıması vb. için ekranın belli bir kısmını kırpmak için kullanılır.
	 */
	public void render(List<Entity> entities, ICamera camera, Vector3f lightDir, Vector4f clipPlane) {
		prepare(camera, lightDir, clipPlane);
		for (Entity entity : entities) {
			prepareSkin(entity.getSkin());
			Vao model = entity.getModel().getVao();
			model.bind(0, 1, 2);
			GL11.glDrawElements(GL11.GL_TRIANGLES, model.getIndexCount(), GL11.GL_UNSIGNED_INT, 0);
			model.unbind(0, 1, 2);
		}
		finish();
	}
	
	/**
	 * Uygulama kapanırken veya sınıf yok edilirken shader kaynaklarını temizler.
	 * Neden: Bellek sızıntılarını (memory leak) önlemek içindir.
	 */
	public void cleanUp(){
		shader.cleanUp();
	}

	/**
	 * Çizim işleminden önce OpenGL ve shader ayarlarını hazırlar.
	 * @param camera Kamera bilgisi (matris hesaplamaları için).
	 * @param lightDir Işık yönü.
	 * @param clipPlane Kırpma düzlemi.
	 */
	private void prepare(ICamera camera, Vector3f lightDir, Vector4f clipPlane) {
		shader.start();
		shader.projectionViewMatrix.loadMatrix(camera.getProjectionViewMatrix());
		shader.lightDirection.loadVec3(lightDir);
		shader.plane.loadVec4(clipPlane);
		OpenGlUtils.antialias(true);
		OpenGlUtils.disableBlending();
		OpenGlUtils.enableDepthTesting(true);
	}

	/**
	 * Çizim işlemi bittikten sonra shader programını durdurur.
	 */
	private void finish() {
		shader.stop();
	}

	/**
	 * Modele ait kaplama (texture) özelliklerini shader'a yükler.
	 * Neden: Her modelin kendine ait resmi ve parlama haritası (glow map vb.) olabilir, bunları hazırlamak gerekir.
	 * @param skin Modele ait kaplama verisi.
	 */
	private void prepareSkin(Skin skin) {
		skin.getDiffuseTexture().bindToUnit(0);
		if (skin.hasExtraMap()) {
			skin.getExtraInfoMap().bindToUnit(1);
		}
		shader.hasExtraMap.loadBoolean(skin.hasExtraMap());
		OpenGlUtils.cullBackFaces(!skin.hasTransparency());
	}

}
