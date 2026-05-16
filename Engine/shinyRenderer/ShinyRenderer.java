package shinyRenderer;

import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

import openglObjects.Vao;
import scene.Entity;
import textures.Texture;
import utils.ICamera;
import utils.OpenGlUtils;

/**
 * Sahnedeki parlak nesneleri (üzerinde çevre yansıması - Environment Map olanlar)
 * render eden (çizen) sınıf.
 */
public class ShinyRenderer {
	
	private ShinyShader shader;

	/**
	 * Parlak nesne renderlayıcısını başlatır.
	 */
	public ShinyRenderer() {
		this.shader = new ShinyShader();
	}

	/**
	 * Belirtilen parlak nesne listesini ekrana çizer.
	 * 
	 * @param shinyEntities Çizilecek parlak nesnelerin listesi
	 * @param enviromap Yansıma efekti için kullanılacak çevresel harita (Küp Haritası / Environment Map)
	 * @param camera Oyuncu kamerası
	 * @param lightDir Sahnenin ana ışık (güneş) yönü
	 */
	public void render(List<Entity> shinyEntities, Texture enviromap, ICamera camera, Vector3f lightDir) {
		prepare(camera, lightDir, enviromap);
		for (Entity entity : shinyEntities) {
			// Objenin kendi rengini 0. Doku birimine bağla
			entity.getSkin().getDiffuseTexture().bindToUnit(0);
			Vao model = entity.getModel().getVao();
			
			// Vertex(0), TextureCoords(1) ve Normal(2) verilerini aktif et
			model.bind(0, 1, 2);
			
			// Objeyi çiz
			GL11.glDrawElements(GL11.GL_TRIANGLES, model.getIndexCount(), GL11.GL_UNSIGNED_INT, 0);
			
			// Çizim bitince VAO kanallarını kapat
			model.unbind(0, 1, 2);
		}
		finish();
	}
	
	/**
	 * Renderer kapatılırken sahip olduğu Shader'ı bellekten siler.
	 */
	public void cleanUp(){
		shader.cleanUp();
	}

	/**
	 * Parlak nesneler çizilmeden önce OpenGL durumlarını (state) ve shader değişkenlerini ayarlar.
	 * 
	 * @param camera Oyuncu kamerası
	 * @param lightDir Işık yönü
	 * @param enviromap Yansıma için kullanılacak çevresel doku
	 */
	private void prepare(ICamera camera, Vector3f lightDir, Texture enviromap) {
		shader.start();
		// Kameranın dönüşüm matrisini shader'a yolla
		shader.projectionViewMatrix.loadMatrix(camera.getProjectionViewMatrix());
		// Işık yönünü shader'a yolla
		shader.lightDirection.loadVec3(lightDir);
		// Kameranın pozisyonunu shader'a yolla (Fresnel etkisi / yansıma açısı hesaplaması için gerekli)
		shader.cameraPosition.loadVec3(camera.getPosition());
		
		// Çevresel yansıma dokusunu 1. Doku birimine bağla
		enviromap.bindToUnit(1);
		
		OpenGlUtils.antialias(true);
		OpenGlUtils.disableBlending();
		OpenGlUtils.enableDepthTesting(true);
		OpenGlUtils.cullBackFaces(true);
	}

	/**
	 * Çizim işlemi bittikten sonra shader kullanımını durdurur.
	 */
	private void finish() {
		shader.stop();
	}

}
