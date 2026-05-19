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

		// Modern Modüler Animasyon Bileşenini ekle
		scene.AnimatorComponent animator = new scene.AnimatorComponent();
		// Kendi etrafında sürekli Y ekseninde dönsün (derece/saniye)
		animator.setContinuousRotation(0f, 45f, 0f);
		// Sürekli havada süzülsün (hız, miktar)
		animator.setBounceEffect(1.5f, 0.4f);
		
		this.addComponent(animator);
	}

	@Override
	protected void onUpdate(float delta) {
		// onUpdate artık boş, tüm hareketler AnimatorComponent bileşeni tarafından yönetiliyor.
	}
}
