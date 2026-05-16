package utils;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.util.WaveData;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * ED Tetris Demo (ve genel projeler) için Modern Ses Yöneticisi (Audio Manager).
 * OpenAL başlatma, tampon (buffer) yönetimi ve oynatma işlevlerini yönetir.
 * Yer tutucu (placeholder) ses dosyaları üretmek için dahili bir ton üreteci (tone generator) içerir.
 */
public class AudioManager {

    // Yüklenen ses verilerini (Buffer) tutan harita
    private static Map<String, Integer> buffers = new HashMap<>();
    // Sesleri çalacak kaynakları (Source) tutan harita
    private static Map<String, Integer> sources = new HashMap<>();
    // Arka plan müziği (BGM) için ayrılmış kaynak kimliği
    private static int bgmSource = -1;
    // Ses sisteminin başlatılıp başlatılmadığını kontrol eder
    private static boolean initialized = false;

    /**
     * OpenAL ses sistemini başlatır.
     */
    public static void init() {
        if (initialized) return;
        try {
            AL.create();
            System.out.println("[AUDIO] OpenAL System Initialized.");
            initialized = true;
            prepareResources();
        } catch (Exception e) {
            System.err.println("[AUDIO] Failed to initialize OpenAL: " + e.getMessage());
        }
    }

    /**
     * Varsayılan ses dosyalarını kontrol eder ve eksikse yer tutucu (placeholder) sesler oluşturur.
     */
    private static void prepareResources() {
        File dir = new File("Resources/audio");
        if (!dir.exists()) dir.mkdirs();

        // Check and generate placeholders if they don't exist
        // (Eksikse yer tutucu ses dosyaları oluştur)
        checkAndGenerate("Resources/audio/land.wav",   220, 0.1, 0.8f);  // Kısa, pes bir yere inme (landing) sesi
        checkAndGenerate("Resources/audio/move.wav",   440, 0.05, 0.3f); // Hafif hareket tıkırtısı (tick)
        checkAndGenerate("Resources/audio/rotate.wav", 880, 0.1, 0.4f);  // Döndürme için "whoosh" benzeri tiz bir ses
        checkAndGenerate("Resources/audio/clear.wav",  660, 0.4, 0.6f);  // Satır silinme (clear) çınlaması

        loadSound("land",   "Resources/audio/land.wav",   false);
        loadSound("move",   "Resources/audio/move.wav",   false);
        loadSound("rotate", "Resources/audio/rotate.wav", false);
        loadSound("clear",  "Resources/audio/clear.wav",  false);
        
        // Load BGM if exists (Varsa arka plan müziğini yükle)
        File bgmFile = new File("Resources/audio/bgm.wav");
        if (bgmFile.exists()) {
            loadSound("bgm", "Resources/audio/bgm.wav", true);
        }
    }

    /** Arka plan müziğini (BGM) başlatır. */
    public static void startBGM() {
        if (!initialized || bgmSource == -1) return;
        AL10.alSourcePlay(bgmSource);
    }

    /** Arka plan müziğini durdurur. */
    public static void stopBGM() {
        if (!initialized || bgmSource == -1) return;
        AL10.alSourceStop(bgmSource);
    }

    /**
     * Belirli bir ses kaynağının ses seviyesini (Gain) ayarlar.
     * @param key Ses anahtarı (örn: "bgm", "land")
     * @param gain Ses seviyesi (0.0f ile 1.0f arası)
     */
    public static void setGain(String key, float gain) {
        if (!initialized || !sources.containsKey(key)) return;
        int source = sources.get(key);
        AL10.alSourcef(source, AL10.AL_GAIN, gain);
    }

    /**
     * İlgili dizinde ses dosyası yoksa otomatik olarak Sine Wave (Sinüs Dalgası) üreterek kaydeder.
     */
    private static void checkAndGenerate(String path, int freq, double duration, float volumeScale) {
        File f = new File(path);
        if (!f.exists()) {
            System.out.println("[AUDIO] Creating placeholder asset: " + path);
            generateSineWav(f, freq, duration, volumeScale);
        }
    }

    /**
     * Bir WAV dosyasını okuyup OpenAL buffer'ına yükler ve ona bir ses kaynağı (Source) tanımlar.
     * 
     * @param key Sesi çağırmak için kullanılacak isim
     * @param path WAV dosyasının yolu
     * @param loop Sesin sürekli tekrar edip etmeyeceği
     */
    public static void loadSound(String key, String path, boolean loop) {
        if (!initialized) return;
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(path));
            WaveData waveFile = WaveData.create(in);
            in.close();

            if (waveFile == null) {
                System.err.println("[AUDIO] Failed to parse WAV: " + path);
                return;
            }

            int buffer = AL10.alGenBuffers();
            AL10.alBufferData(buffer, waveFile.format, waveFile.data, waveFile.samplerate);
            waveFile.dispose();

            int source = AL10.alGenSources();
            AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
            AL10.alSourcef(source, AL10.AL_PITCH, 1.0f);
            AL10.alSourcef(source, AL10.AL_GAIN, 1.0f);
            if (loop) AL10.alSourcei(source, AL10.AL_LOOPING, AL10.AL_TRUE);

            buffers.put(key, buffer);
            sources.put(key, source);
            
            if (key.equals("bgm")) bgmSource = source;

        } catch (Exception e) {
            System.err.println("[AUDIO] Error loading " + path + ": " + e.getMessage());
        }
    }

    /**
     * Yüklenmiş bir ses efektini (SFX) oynatır.
     * @param key Oynatılacak sesin anahtarı (Örn: "move", "clear")
     */
    public static void playSFX(String key) {
        if (!initialized) return;
        if (sources.containsKey(key)) {
            int source = sources.get(key);
            // Stop first to allow rapid re-triggering (like many lines clearing at once)
            // (Sesin üst üste seri bir şekilde çalınabilmesi için önce durduruyoruz)
            AL10.alSourceStop(source);
            AL10.alSourcePlay(source);
        }
    }

    /**
     * OpenAL kaynaklarını (Source) ve tamponlarını (Buffer) temizler, sistemi kapatır.
     */
    public static void cleanup() {
        if (!initialized) return;
        for (int source : sources.values()) {
            AL10.alSourceStop(source);
            AL10.alDeleteSources(source);
        }
        for (int buffer : buffers.values()) {
            AL10.alDeleteBuffers(buffer);
        }
        AL.destroy();
        initialized = false;
        System.out.println("[AUDIO] OpenAL System Shutdown.");
    }

    // --- INTERNAL UTILITY: SINE WAVE WAV GENERATOR ---
    // --- DAHİLİ ARAÇ: SİNÜS DALGASI WAV ÜRETECİ ---
    /**
     * Verilen frekans ve sürede bir sinüs dalgası (WAV dosyası) üretir.
     */
    private static void generateSineWav(File file, int freq, double duration, float vol) {
        int sampleRate = 44100;
        int numSamples = (int) (duration * sampleRate);
        short[] samples = new short[numSamples];
        for (int i = 0; i < numSamples; i++) {
            // Sine wave formula: A * sin(2 * PI * f * t)
            // (Sinüs dalgası formülü)
            samples[i] = (short) (Math.sin(2 * Math.PI * i * freq / sampleRate) * 32767 * vol);
        }
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
            // RIFF header
            out.writeBytes("RIFF");
            out.writeInt(Integer.reverseBytes(36 + numSamples * 2));
            out.writeBytes("WAVE");
            // fmt chunk
            out.writeBytes("fmt ");
            out.writeInt(Integer.reverseBytes(16));
            out.writeShort(Short.reverseBytes((short) 1)); // PCM format
            out.writeShort(Short.reverseBytes((short) 1)); // Mono channel
            out.writeInt(Integer.reverseBytes(sampleRate));
            out.writeInt(Integer.reverseBytes(sampleRate * 2)); // Byte rate
            out.writeShort(Short.reverseBytes((short) 2)); // Block align
            out.writeShort(Short.reverseBytes((short) 16)); // Bits per sample
            // data chunk
            out.writeBytes("data");
            out.writeInt(Integer.reverseBytes(numSamples * 2));
            for (short s : samples) {
                out.writeShort(Short.reverseBytes(s)); // Little endian short (Küçük Sonlu 16-bit veri)
            }
        } catch (IOException e) {
            System.err.println("[AUDIO] Failed to generate placeholder " + file.getName());
        }
    }
}
