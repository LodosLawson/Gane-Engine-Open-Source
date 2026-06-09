package skybox.atmosphere;

import org.lwjgl.util.vector.Vector3f;

/**
 * Fizik tabanlÄ± bulut kumesi.
 *
 * Her bulut kumesi:
 *  - Kendi konumunu, hizini ve omrunu bilir.
 *  - Ruzgar yonunde hareket eder + kendi bireysel turbulans sapi.
 *  - Diger bulutlara yakin oldugunda "karanlik yoÄŸunluk" (density) artar.
 *  - Dogum (fade-in) ve olum (fade-out) ile alpha kademeli degisir.
 *  - Her kume kendi tohum (seed) degerine sahiptir â€” tekrar yok.
 */
public class CloudCluster {

    // --- Durum ---
    /** Dunya koordinatindaki merkez konumu */
    public final Vector3f position = new Vector3f();

    /** 0-1 arasi saydamlik: 0=yeni dogdu/oldu, 1=tam gorunur */
    public float alpha = 0f;

    /** 0-1 arasi yogunluk: Yakindaki bulutlar artirinca karanligini arttirir */
    public float density = 0f;

    /** Bulut parcaciklari: her eleman (offsetX, offsetY, offsetZ, radius) */
    public final org.lwjgl.util.vector.Vector4f[] particles;

    // --- Yasam dongÃ¼sÃ¼ ---
    /** Toplam yasam suresi (saniye) */
    private float lifetime;
    /** Gecen sure */
    private float age = 0f;
    /** DoguÅŸ ve oluÅŸ icin fade suresi */
    private static final float FADE_TIME = 12f;

    // --- Bireysel kaos ---
    /** Baz aciyi turbulans icin (her bulut farkli titresiyor) */
    private float turbulancePhase;
    private float turbulanceFreq;
    private float turbulanceAmp;

    // --- Birlesme/DagÄ±lma ---
    /** Bu kume silinmek uzere mi (alpha sifira dusuyor) */
    boolean dying = false;

    /** Kumenin olcegi (buyukluk carpani, genellikle 0.8 ile 1.4 arasi) */
    public final float scale;

    /** Dinamik olcek (birlesmelerle degisir) */
    public float currentScale;

    // --- Katman ve Fizik Parametreleri ---
    public int layer = 0; // 0: AlÃ§ak (Cumulus), 1: Orta (Altocumulus), 2: YÃ¼ksek (Cirrus)
    private float speedFactor;
    private float drift;
    private float verticalSpeed;

    /**
     * @param x, y, z  Baslangic konumu
     * @param windDir   Ruzgar yonu vektoru (normalize edilmis)
     * @param windSpeed Ruzgar hizi (birim/saniye)
     * @param seed      Benzersiz rastgele tohum
     * @param scaleMultiplier Olcek carpani
     * @param layer     Bulutun ait oldugu katman (0, 1, 2)
     */
    public CloudCluster(float x, float y, float z,
                        Vector3f windDir, float windSpeed,
                        long seed, float scaleMultiplier, int layer) {
        this.position.set(x, y, z);
        this.layer = layer;

        // Basit pseudo-random â€” seed'e gore farkli degerler
        float r1 = pseudoRand(seed, 1);
        float r2 = pseudoRand(seed, 2);
        float r3 = pseudoRand(seed, 3);
        float r4 = pseudoRand(seed, 4);
        float r5 = pseudoRand(seed, 5);
        float r6 = pseudoRand(seed, 6);
        float r7 = pseudoRand(seed, 7);

        // Ruzgar hizinin %70-%130'i kadar hizli git, kucuk sapma acisi ile
        this.speedFactor = 0.70f + r1 * 0.60f;
        this.drift = (r2 - 0.5f) * 0.35f; // Â±17 derece sapma
        this.verticalSpeed = (r3 - 0.5f) * 2f; // hafif dikey salinti

        this.lifetime = 120f + r4 * 240f; // YÃ¼ksek bulutlar iÃ§in daha uzun yaÅŸam (2 - 6 dakika)
        this.turbulancePhase = r5 * 100f;
        this.turbulanceFreq  = 0.08f + r6 * 0.12f;
        this.turbulanceAmp   = 8f + r7 * 18f;

        // Katmana gÃ¶re ek Ã¶lÃ§ek ve hÄ±z azaltmalarÄ± (paralaks hissi ve rÃ¼zgar direnci iÃ§in)
        float layerScale = 1.0f;
        if (layer == 1) {
            layerScale = 0.75f;
            this.speedFactor *= 0.75f;
        } else if (layer == 2) {
            layerScale = 0.50f;
            this.speedFactor *= 0.50f;
        }

        this.scale = (0.75f + pseudoRand(seed, 8) * 0.65f) * scaleMultiplier * layerScale;
        this.currentScale = scale;

        // Parcacik sayisi ve sekli
        int particleCount = 12 + (int)(r1 * 14); // 12-25 arasi
        this.particles = new org.lwjgl.util.vector.Vector4f[particleCount];
        buildParticles(seed);
    }

    private void buildParticles(long seed) {
        float width  = 200f + pseudoRand(seed, 10) * 250f;  // Yatay genislik
        float height = 35f  + pseudoRand(seed, 11) * 45f;   // Dikey yukseklik
        for (int i = 0; i < particles.length; i++) {
            float r = pseudoRand(seed + i * 31L, 0);
            float angle = r * 2f * (float)Math.PI;
            float dist  = pseudoRand(seed + i * 31L, 1) * width;
            float ox = (float)Math.cos(angle) * dist;
            float oz = (float)Math.sin(angle) * dist;
            float oy = (pseudoRand(seed + i * 31L, 2) - 0.3f) * height;
            float radius = (50f + pseudoRand(seed + i * 31L, 3) * 80f) * scale;
            particles[i] = new org.lwjgl.util.vector.Vector4f(ox, oy, oz, radius);
        }
    }

    /**
     * Fizik guncelleme â€” her frame cagrilir.
     * @param delta       Gecen sure (saniye)
     * @param windDir     Sahnedeki ruzgar yonu
     * @param windSpeed   Ruzgar hizi
     * @param time        Toplam gecen sure (turbulans fazlari icin)
     * @param nearbyCount Yakin bolgdeki baska bulut sayisi (karanlik icin)
     */
    public void update(float delta, Vector3f windDir, float windSpeed, float time, int nearbyCount) {
        age += delta;

        // Yalniz kalan bulutlarin omru biraz daha hizli bitsin (dagilma)
        if (nearbyCount == 0 && !dying) {
            age += delta * 0.4f; // %40 daha hizli yaslanma
        }

        // --- Turbulans (Perlin benzeri sin/cos kaos) ---
        float turb = (float)(
            Math.sin((time + turbulancePhase) * turbulanceFreq) * turbulanceAmp +
            Math.cos((time + turbulancePhase * 1.3f) * turbulanceFreq * 1.7f) * turbulanceAmp * 0.4f
        );

        // Katmana gÃ¶re rÃ¼zgar yÃ¶nÃ¼ kaymasÄ± (Wind Shear)
        float angleOffset = 0f;
        if (layer == 1) angleOffset = 0.26f; // +15 derece (radyan)
        else if (layer == 2) angleOffset = -0.52f; // -30 derece (radyan)

        float cosA = (float)Math.cos(angleOffset);
        float sinA = (float)Math.sin(angleOffset);
        float rx = windDir.x * cosA - windDir.z * sinA;
        float rz = windDir.x * sinA + windDir.z * cosA;

        // Dinamik olarak rÃ¼zgar hÄ±zÄ±na gÃ¶re hareketi hesapla
        float vx = rx * windSpeed * speedFactor - rz * drift;
        float vz = rz * windSpeed * speedFactor + rx * drift;

        // Konumu guncelle
        position.x += (vx + turb * 0.3f) * delta;
        position.z += vz * delta;
        position.y += (float)(Math.sin((time + turbulancePhase) * turbulanceFreq * 0.5f) * 1.5f + verticalSpeed) * delta;

        // --- Alpha (Dogus / Olus ---
        if (!dying) {
            if (age < FADE_TIME) {
                alpha = age / FADE_TIME; // Fade in
            } else if (age > lifetime - FADE_TIME) {
                alpha = Math.max(0f, (lifetime - age) / FADE_TIME); // Fade out
            } else {
                alpha = 1.0f;
            }
        } else {
            alpha -= delta / (FADE_TIME * 0.5f); // Hizli olum
            currentScale -= delta * 0.15f * scale; // Olurken yavasca kucul
            if (currentScale < 0.0f) currentScale = 0.0f;
        }
        alpha = Math.max(0f, Math.min(1f, alpha));

        // Katmana gÃ¶re maksimum saydamlÄ±k sÄ±nÄ±rÄ± (yÃ¼ksek bulutlar cirrus gibi ince ve saydam)
        float maxAlpha = 1.0f;
        if (layer == 1) maxAlpha = 0.75f;
        else if (layer == 2) maxAlpha = 0.45f;
        alpha = alpha * maxAlpha;

        // --- Yogunluk (Yakin bulucularin etkisi) ---
        float targetDensity = Math.min(1f, nearbyCount * 0.25f); // Her yakin bulut %25 karanlik ekler
        density += (targetDensity - density) * delta * 0.3f; // Yumusak gecis

        // Olusu belirle
        if (age >= lifetime) dying = true;
    }

    /** Bu kume artik tamamen hayattan gecikmeli ve silinebilir. */
    public boolean isDead() {
        return (dying && alpha <= 0.01f) || (currentScale <= 0.01f);
    }

    /** Bu kumenin baslangictan sonra ne kadar yasadigini dondurur. */
    public float getAge() { return age; }
    
    /** Baslangic yasamini ileri sarmak icin kullanilir (ornegin ilk yuklemede hazir bulutlar) */
    public void setAge(float age) { this.age = age; }

    // Deterministik pseudo-random: 0-1 arasi
    private static float pseudoRand(long seed, int offset) {
        long n = (seed * 1664525L + 1013904223L + offset * 6364136L) & 0x7fffffffL;
        return (n % 100000) / 100000.0f;
    }
}

