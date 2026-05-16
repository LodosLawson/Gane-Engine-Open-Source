package skybox;

import org.lwjgl.opengl.GL11;

import openglObjects.Vao;
import utils.ICamera;
import utils.OpenGlUtils;

/**
 * Gökyüzünü (Skybox) ekrana çizen (render eden) özel sınıf.
 * Gökyüzü her zaman diğer objelerin en arkasında kalacak şekilde derinlik testleri ayarlanarak çizilir.
 */
public class SkyboxRenderer {
	
	private SkyboxShader shader;
	
	/**
	 * Skybox renderlayıcısını başlatır.
	 */
	public SkyboxRenderer(){
		this.shader = new SkyboxShader();
	}
	
	/**
	 * Belirtilen gökyüzünü kameranın bakış açısına göre ekrana çizer.
	 * 
	 * @param skybox Çizilecek gökyüzü objesi
	 * @param camera Oyuncu kamerası
	 */
	public void render(Skybox skybox, ICamera camera){
		if (skybox == null) {
			return;
		}
		prepare(skybox, camera);
		Vao model = skybox.getCubeVao();
		
		// 0 numaralı attribute (Köşe pozisyonları) aktif edilir
		model.bind(0);
		// Küp çizilir
		GL11.glDrawElements(GL11.GL_TRIANGLES, model.getIndexCount(), GL11.GL_UNSIGNED_INT, 0);
		model.unbind(0);
		
		finish();
	}
	
	/**
	 * Renderer kapatılırken gökyüzü shader'ını bellekten siler.
	 */
	public void cleanUp(){
		shader.cleanUp();
	}
	
	/**
	 * Çizim öncesi OpenGL durumlarını hazırlar. Gökyüzünün arkada kalması için
	 * derinlik tamponuna (Depth Buffer) yazılması engellenir.
	 * 
	 * @param skybox Gökyüzü nesnesi
	 * @param camera Oyuncu kamerası
	 */
	private void prepare(Skybox skybox, ICamera camera){
		shader.start();
		// Skybox çizilirken depth buffer'a yazmayı kapat (Diğer nesneler onu ezebilsin diye)
		GL11.glDepthMask(false);
		
		shader.projectionViewMatrix.loadMatrix(camera.getProjectionViewMatrix());
		skybox.getTexture().bindToUnit(0);
		
		OpenGlUtils.disableBlending();
		OpenGlUtils.enableDepthTesting(true);
		// Küpün içinden dışına doğru baktığımız için Back-face culling (arka yüz gizlemeyi) açıyoruz
		OpenGlUtils.cullBackFaces(true);
		OpenGlUtils.antialias(false);
	}
	
	/**
	 * Çizim işlemi bittikten sonra derinlik maskesini (Depth Mask) eski haline getirir ve shader'ı kapatır.
	 */
	private void finish(){
		// Depth buffer'a yazmayı tekrar aktif et
		GL11.glDepthMask(true);
		shader.stop();
	}	

}
