package loaders;

import java.io.BufferedReader;

import utils.MyFile;

/**
 * Nesnelerin ayar (configs) dosyalarını okuyup ayrıştıran sınıf.
 */
public class ConfigsLoader {
	
	/**
	 * Verilen konfigürasyon dosyasını okuyarak Configs nesnesini oluşturur.
	 * 
	 * @param configsFile Okunacak olan configs.txt dosyası
	 * @return Dosyadan okunan değerlere göre doldurulmuş Configs nesnesi
	 */
	public Configs loadConfigs(MyFile configsFile){
		BufferedReader reader = null;
		Configs configs = new Configs();
		try {
			reader = configsFile.getReader();
			createConfigs(reader, configs);
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Couldn't load configs file: " + configsFile);
		}
		return configs;
	}
	
	/**
	 * Configs nesnesinin değişkenlerini okuyucudan satır satır alarak atar.
	 * 
	 * @param reader Dosyayı okuyan nesne
	 * @param configs Doldurulacak olan config objesi
	 * @throws Exception Dosya okunurken hata oluşursa
	 */
	private void createConfigs(BufferedReader reader, Configs configs) throws Exception{
		configs.setExtraMap(readNextBoolean(reader));
		configs.setTransparency(readNextBoolean(reader));
		configs.setReflection(readNextBoolean(reader));
		configs.setRefraction(readNextBoolean(reader));
		configs.setCastsShadow(readNextBoolean(reader));
		configs.setImportant(readNextBoolean(reader));
	}
	
	/**
	 * Sıradaki satırı okuyarak TRUE veya FALSE olmasına göre boolean döndürür.
	 * Değer formata (ANAHTAR;TRUE/FALSE) göre ayırıcıdan (;) sonrasından alınır.
	 * 
	 * @param reader Dosyayı okuyan nesne
	 * @return Okunan mantıksal değer
	 * @throws Exception Dosya okunurken hata oluşursa
	 */
	private boolean readNextBoolean(BufferedReader reader) throws Exception{
		String line = reader.readLine();
		String bool = line.split(LoaderSettings.SEPARATOR)[1];
		return bool.equals(LoaderSettings.TRUE);
	}

}
