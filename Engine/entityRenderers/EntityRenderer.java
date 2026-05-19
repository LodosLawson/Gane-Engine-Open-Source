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
	public void render(List<Entity> entities, ICamera camera, scene.Scene scene, Vector4f clipPlane) {
		prepare(camera, scene, clipPlane);
		for (Entity entity : entities) {
			prepareSkin(entity.getSkin());
			Vao model = entity.getModel().getVao();
			
			org.lwjgl.util.vector.Matrix4f transform = new org.lwjgl.util.vector.Matrix4f();
			transform.setIdentity();
			org.lwjgl.util.vector.Matrix4f.translate(entity.getPosition(), transform, transform);
			
			// Rotation X, Y, Z (in degrees)
			if (entity.getRotation().x != 0) {
				org.lwjgl.util.vector.Matrix4f.rotate((float) Math.toRadians(entity.getRotation().x), new org.lwjgl.util.vector.Vector3f(1, 0, 0), transform, transform);
			}
			if (entity.getRotation().y != 0) {
				org.lwjgl.util.vector.Matrix4f.rotate((float) Math.toRadians(entity.getRotation().y), new org.lwjgl.util.vector.Vector3f(0, 1, 0), transform, transform);
			}
			if (entity.getRotation().z != 0) {
				org.lwjgl.util.vector.Matrix4f.rotate((float) Math.toRadians(entity.getRotation().z), new org.lwjgl.util.vector.Vector3f(0, 0, 1), transform, transform);
			}
			
			// Scale (uniformly)
			if (entity.getScale() != 1.0f) {
				org.lwjgl.util.vector.Matrix4f.scale(new org.lwjgl.util.vector.Vector3f(entity.getScale(), entity.getScale(), entity.getScale()), transform, transform);
			}
			
			shader.transformationMatrix.loadMatrix(transform);
			
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
	private void prepare(ICamera camera, scene.Scene scene, Vector4f clipPlane) {
		shader.start();
		shader.projectionViewMatrix.loadMatrix(camera.getProjectionViewMatrix());
		shader.lightDirection.loadVec3(scene.getLightDirection());
		shader.lightColor.loadVec3(scene.getLightColor());
		shader.lightBrightness.loadFloat(scene.getLightBrightness());
		shader.ambientLight.loadFloat(scene.getAmbientLight());
		shader.plane.loadVec4(clipPlane);
		shader.cameraPosition.loadVec3(camera.getPosition());
		
		// Eğer sahnede nokta ışık varsa shader'a yükle, yoksa siyah renk (0,0,0) gönder
		scene.Light pointLight = scene.getPointLight();
		if(pointLight != null) {
			shader.pointLightPos.loadVec3(pointLight.getPosition());
			shader.pointLightColor.loadVec3(pointLight.getColor());
			shader.pointLightAttenuation.loadVec3(pointLight.getAttenuation());
		} else {
			shader.pointLightColor.loadVec3(new Vector3f(0, 0, 0));
			shader.pointLightAttenuation.loadVec3(new Vector3f(1, 0, 0)); // 0'a bölünme (NaN) hatasını önlemek için sabit 1 gönder!
		}
		
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
