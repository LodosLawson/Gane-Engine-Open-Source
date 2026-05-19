package gane.objects;

import scene.GameObject;

/**
 * Modern "Bileşen ve Kalıtım" mimarisiyle oluşturulmuş,
 * kendi modelini ve kaplamasını kendi içinde taşıyan tam bağımsız ahşap zemin objesi.
 */
public class AhsapZemin extends GameObject {

	public AhsapZemin() {
		// Objeye özel 3D Model ve PBR (Color + Roughness) Texture haritalarını tek satırda yükle
		super("res/Başlıksız.obj", "res/WoodFloor004_4K-PNG_Color.png", "res/WoodFloor004_4K-PNG_Roughness.png");
		
		// Cam / Saydamlık özelliklerini ayarla
		this.getSkin().setTransparent(true);
		
		// Başlangıç lokasyonu (MainApp'te oluşturduğumuzda burada doğacak)
		this.getPosition().set(-5f, 0f, -5f); 
	}

	@Override
	protected void onUpdate(float delta) {
		// Objenin her saniye yapacağı hareket / mantık buraya yazılır.
		// Örnek: Kendi ekseni etrafında hafifçe dönme ve havada süzülme (Bobbing efekti)
		float time = System.currentTimeMillis() / 1000.0f;
		
		// Y ekseninde (yukarı-aşağı) yavaşça süzül
		this.getPosition().y = (float) Math.sin(time * 2.0f) * 0.5f;
	}
}
