package skybox.classic;

import org.lwjgl.opengl.GL11;

import openglObjects.Vao;
import utils.ICamera;
import utils.OpenGlUtils;

/**
 * GÃ¶kyÃ¼zÃ¼nÃ¼ (Skybox) ekrana Ã§izen (render eden) Ã¶zel sÄ±nÄ±f.
 * GÃ¶kyÃ¼zÃ¼ her zaman diÄŸer objelerin en arkasÄ±nda kalacak ÅŸekilde derinlik testleri ayarlanarak Ã§izilir.
 */
public class SkyboxRenderer {
	
	private SkyboxShader shader;
	private final org.lwjgl.util.vector.Matrix4f reusableViewMatrix = new org.lwjgl.util.vector.Matrix4f();
	private final org.lwjgl.util.vector.Matrix4f reusableProjectionViewMatrix = new org.lwjgl.util.vector.Matrix4f();

	
	/**
	 * Skybox renderlayÄ±cÄ±sÄ±nÄ± baÅŸlatÄ±r.
	 */
	public SkyboxRenderer(){
		this.shader = new SkyboxShader();
	}
	
	/**
	 * Belirtilen gÃ¶kyÃ¼zÃ¼nÃ¼ kameranÄ±n bakÄ±ÅŸ aÃ§Ä±sÄ±na gÃ¶re ekrana Ã§izer.
	 * 
	 * @param skybox Ã‡izilecek gÃ¶kyÃ¼zÃ¼ objesi
	 * @param camera Oyuncu kamerasÄ±
	 */
	public void render(Skybox skybox, ICamera camera){
		if (skybox == null) {
			return;
		}
		prepare(skybox, camera);
		Vao model = skybox.getCubeVao();
		
		// 0 numaralÄ± attribute (KÃ¶ÅŸe pozisyonlarÄ±) aktif edilir
		model.bind(0);
		// KÃ¼p Ã§izilir
		GL11.glDrawElements(GL11.GL_TRIANGLES, model.getIndexCount(), GL11.GL_UNSIGNED_INT, 0);
		model.unbind(0);
		
		finish();
	}
	
	/**
	 * Renderer kapatÄ±lÄ±rken gÃ¶kyÃ¼zÃ¼ shader'Ä±nÄ± bellekten siler.
	 */
	public void cleanUp(){
		shader.cleanUp();
	}
	
	/**
	 * Ã‡izim Ã¶ncesi OpenGL durumlarÄ±nÄ± hazÄ±rlar. GÃ¶kyÃ¼zÃ¼nÃ¼n arkada kalmasÄ± iÃ§in
	 * derinlik tamponuna (Depth Buffer) yazÄ±lmasÄ± engellenir.
	 * 
	 * @param skybox GÃ¶kyÃ¼zÃ¼ nesnesi
	 * @param camera Oyuncu kamerasÄ±
	 */
	private void prepare(Skybox skybox, ICamera camera){
		shader.start();
		// Skybox Ã§izilirken depth buffer'a yazmayÄ± kapat (DiÄŸer nesneler onu ezebilsin diye)
		GL11.glDepthMask(false);
		
		reusableViewMatrix.load(camera.getViewMatrix());
		reusableViewMatrix.m30 = 0;
		reusableViewMatrix.m31 = 0;
		reusableViewMatrix.m32 = 0;
		
		// Ã–zel view matrix ile projection matrix'i Ã§arpÄ±p shader'a gÃ¶nder
		org.lwjgl.util.vector.Matrix4f.mul(camera.getProjectionMatrix(), reusableViewMatrix, reusableProjectionViewMatrix);
		shader.projectionViewMatrix.loadMatrix(reusableProjectionViewMatrix);

		
		skybox.getTexture().bindToUnit(0);
		
		OpenGlUtils.disableBlending();
		OpenGlUtils.enableDepthTesting(true);
		// KÃ¼pÃ¼n iÃ§inden dÄ±ÅŸÄ±na doÄŸru baktÄ±ÄŸÄ±mÄ±z iÃ§in Back-face culling (arka yÃ¼z gizlemeyi) aÃ§Ä±yoruz
		OpenGlUtils.cullBackFaces(true);
		OpenGlUtils.antialias(false);
	}
	
	/**
	 * Ã‡izim iÅŸlemi bittikten sonra derinlik maskesini (Depth Mask) eski haline getirir ve shader'Ä± kapatÄ±r.
	 */
	private void finish(){
		// Depth buffer'a yazmayÄ± tekrar aktif et
		GL11.glDepthMask(true);
		shader.stop();
		org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
		GL11.glBindTexture(org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP, 0);
	}	

}

