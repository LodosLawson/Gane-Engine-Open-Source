package utils;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.util.WaveData;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Modern Audio Manager for ED Tetris Demo.
 * Handles OpenAL initialization, buffer management, and playback.
 * Includes a built-in tone generator for placeholder sound assets.
 */
public class AudioManager {

    private static Map<String, Integer> buffers = new HashMap<>();
    private static Map<String, Integer> sources = new HashMap<>();
    private static int bgmSource = -1;
    private static boolean initialized = false;

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

    private static void prepareResources() {
        File dir = new File("Resources/audio");
        if (!dir.exists()) dir.mkdirs();

        // Check and generate placeholders if they don't exist
        checkAndGenerate("Resources/audio/land.wav",   220, 0.1, 0.8f);  // Short low landing blip
        checkAndGenerate("Resources/audio/move.wav",   440, 0.05, 0.3f); // Subtle tick for movement
        checkAndGenerate("Resources/audio/rotate.wav", 880, 0.1, 0.4f);  // Whoosh-like pitch for rotation
        checkAndGenerate("Resources/audio/clear.wav",  660, 0.4, 0.6f);  // High-pitch chime for clear

        loadSound("land",   "Resources/audio/land.wav",   false);
        loadSound("move",   "Resources/audio/move.wav",   false);
        loadSound("rotate", "Resources/audio/rotate.wav", false);
        loadSound("clear",  "Resources/audio/clear.wav",  false);
        
        // Load BGM if exists
        File bgmFile = new File("Resources/audio/bgm.wav");
        if (bgmFile.exists()) {
            loadSound("bgm", "Resources/audio/bgm.wav", true);
        }
    }

    public static void startBGM() {
        if (!initialized || bgmSource == -1) return;
        AL10.alSourcePlay(bgmSource);
    }

    public static void stopBGM() {
        if (!initialized || bgmSource == -1) return;
        AL10.alSourceStop(bgmSource);
    }

    public static void setGain(String key, float gain) {
        if (!initialized || !sources.containsKey(key)) return;
        int source = sources.get(key);
        AL10.alSourcef(source, AL10.AL_GAIN, gain);
    }

    private static void checkAndGenerate(String path, int freq, double duration, float volumeScale) {
        File f = new File(path);
        if (!f.exists()) {
            System.out.println("[AUDIO] Creating placeholder asset: " + path);
            generateSineWav(f, freq, duration, volumeScale);
        }
    }

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

    public static void playSFX(String key) {
        if (!initialized) return;
        if (sources.containsKey(key)) {
            int source = sources.get(key);
            // Stop first to allow rapid re-triggering (like many lines clearing at once)
            AL10.alSourceStop(source);
            AL10.alSourcePlay(source);
        }
    }

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
    private static void generateSineWav(File file, int freq, double duration, float vol) {
        int sampleRate = 44100;
        int numSamples = (int) (duration * sampleRate);
        short[] samples = new short[numSamples];
        for (int i = 0; i < numSamples; i++) {
            // Sine wave formula: A * sin(2 * PI * f * t)
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
                out.writeShort(Short.reverseBytes(s)); // Little endian short
            }
        } catch (IOException e) {
            System.err.println("[AUDIO] Failed to generate placeholder " + file.getName());
        }
    }
}
