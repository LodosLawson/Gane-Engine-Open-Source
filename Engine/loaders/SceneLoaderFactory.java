package loaders;

/**
 * SceneLoader sınıfını oluşturmayı ve tüm bağımlı alt yükleyici sınıfları
 * ayarlamayı sağlayan basit fabrika (Factory) sınıfı.
 */
public class SceneLoaderFactory {

	/**
	 * Tüm gerekli yükleyicileri (Model, Skin, Configs, vb.) başlatıp birbirine bağlayarak, 
	 * yeni bir SceneLoader örneği döndürür.
	 * 
	 * @return Sahneyi yüklemeye yarayan yapılandırılmış nesne.
	 */
	public static SceneLoader createSceneLoader() {
		/*
		 * Orijinal notun çevirisi:
		 * Birden çok loader seçeceğimiz zaman veya loader'ların çeşitli ayarları 
		 * olsaydı bu sınıf daha işlevsel olurdu. Örneğin, eğer farklı model formatları 
		 * desteklenseydi, burada farklı türde bir ModelLoader oluşturulabilirdi.
		 * Bu aşamada biraz yersiz ve aşırıya kaçılmış görünebilir ancak kendi başıma
		 * bir pratik (alıştırma) olması için bu şekilde yaptım :P
		 */
		ModelLoader modelLoader = new ModelLoader();
		SkinLoader skinLoader = new SkinLoader();
		ConfigsLoader configsLoader = new ConfigsLoader();
		EntityLoader entityLoader = new EntityLoader(modelLoader, skinLoader, configsLoader);
		SkyboxLoader skyLoader = new SkyboxLoader();
		return new SceneLoader(entityLoader, skyLoader);
	}

}
