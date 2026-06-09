package water;

import java.util.Iterator;
import java.util.List;

import scene.Entity;
import particles.ParticleManager;

/**
 * Objeler ile su yüzeyi arasındaki etkileşimi yöneten sistem.
 *
 * <p>Her karede sahnedeki entity'lerin Y pozisyonlarını bir önceki karenin
 * pozisyonuyla karşılaştırır. Su yüzeyini kesen bir entity tespit edildiğinde
 * otomatik olarak ripple (dalga halkası) üretir.</p>
 *
 * <p>Üretilen ripple verileri shader'a float array olarak iletilir;
 * vertex shader su yüzeyini bu merkezlere olan mesafeye göre büker,
 * böylece gerçekçi genişleyen halka etkisi oluşur.</p>
 *
 * <h3>Kullanım:</h3>
 * <pre>
 *   // Scene.updateWaterInteractions(delta) otomatik çağırır.
 *   // Doğrudan kullanmak için:
 *   waterTile.getInteractionSystem().spawnRipple(x, z, 0.4f);
 * </pre>
 */
public class WaterInteractionSystem {

    /** Shader array boyutu — GLSL'deki MAX_RIPPLES ile aynı olmalı */
    public static final int MAX_RIPPLES = WaterRipple.MAX_RIPPLES;

    // Su yüzeyi yüksekliği (Y ekseni)
    private final float waterHeight;

    // Aktif dalga halkaları
    private final List<WaterRipple> ripples = new java.util.ArrayList<>();

    // Her entity'nin bir önceki karede ki Y pozisyonu
    // (Yüzey geçişini tespit etmek için gerekli)
    private final java.util.Map<Entity, Float> previousY = new java.util.IdentityHashMap<>();

    // Dalga parametreleri (override edilebilir)
    private float defaultStrength  = 0.35f;  // Giriş dalgası genliği
    private float defaultSpeed     = 7.0f;   // Yayılma hızı (birim/sn)
    private float defaultMaxRadius = 12.0f;  // Maksimum yarıçap

    /**
     * @param waterHeight Su yüzeyinin Y eksenindeki yüksekliği
     */
    public WaterInteractionSystem(float waterHeight) {
        this.waterHeight = waterHeight;
    }

    // ==================== Güncelleme ====================

    /**
     * Her oyun karesinde çağrılır.
     * <ol>
     *   <li>Entity'lerin su yüzeyini geçip geçmediğini kontrol eder</li>
     *   <li>Mevcut ripple'ların yaşını günceller ve ölenleri temizler</li>
     * </ol>
     *
     * @param entities Sahnedeki tüm entity'lerin listesi
     * @param delta    Delta time (saniye)
     */
    public void update(List<Entity> entities, float delta) {
        // 1. Entity geçiş tespiti
        checkWaterCrossings(entities, delta);

        // 2. Ripple'ları güncelle ve ölüleri temizle
        Iterator<WaterRipple> it = ripples.iterator();
        while (it.hasNext()) {
            WaterRipple r = it.next();
            r.update(delta);
            if (r.isDead()) {
                it.remove();
            }
        }
    }

    /**
     * Entity'lerin su yüzeyini geçip geçmediğini kontrol eder.
     * Önceki frame Y'si > waterHeight && bu frame Y'si < waterHeight → GIRIŞ
     * Önceki frame Y'si < waterHeight && bu frame Y'si > waterHeight → ÇIKIŞ
     */
    private void checkWaterCrossings(List<Entity> entities, float delta) {
        for (Entity entity : entities) {
            float currentY = entity.getPosition().y;

            // Bu entity'yi daha önce gördük mü?
            if (!previousY.containsKey(entity)) {
                previousY.put(entity, currentY);
                continue;
            }

            float prevY = previousY.get(entity);

            // Giriş: Yukarıdan aşağıya su yüzeyini geçti
            if (prevY > waterHeight && currentY <= waterHeight) {
                float speed = 0.0f;
                if (delta > 0.0001f) {
                    speed = Math.abs(prevY - currentY) / delta; // Hıza göre ölçekle
                }
                if (Float.isNaN(speed) || Float.isInfinite(speed) || speed < 0f) {
                    speed = 5.0f;
                } else if (speed > 50.0f) {
                    speed = 50.0f;
                }
                float str = Math.min(defaultStrength * (1f + speed * 0.05f), 0.8f);
                spawnRipple(entity.getPosition().x, entity.getPosition().z, str, defaultSpeed, defaultMaxRadius);
                ParticleManager.getInstance().spawnSplash(entity.getPosition().x, waterHeight, entity.getPosition().z, speed);
                System.out.println("[Water] Entry splash at (" +
                    String.format("%.1f", entity.getPosition().x) + ", " +
                    String.format("%.1f", entity.getPosition().z) + ") strength=" +
                    String.format("%.2f", str));
            }
            // Çıkış: Aşağıdan yukarıya su yüzeyini geçti
            else if (prevY < waterHeight && currentY > waterHeight) {
                spawnRipple(entity.getPosition().x, entity.getPosition().z,
                    defaultStrength * 0.5f, defaultSpeed * 0.7f, defaultMaxRadius * 0.6f);
                ParticleManager.getInstance().spawnSplash(entity.getPosition().x, waterHeight, entity.getPosition().z, 2.5f);
            }

            previousY.put(entity, currentY);
        }
    }

    // ==================== Ripple Üretimi ====================

    /**
     * Belirtilen dünya konumunda yeni bir ripple başlatır.
     * Aktif ripple sayısı {@link #MAX_RIPPLES}'e ulaştıysa en eski ripple atılır.
     *
     * @param x        Dünya X koordinatı
     * @param z        Dünya Z koordinatı
     * @param strength Dalga genliği (0.1–0.8)
     */
    public void spawnRipple(float x, float z, float strength) {
        spawnRipple(x, z, strength, defaultSpeed, defaultMaxRadius);
    }

    /**
     * Tüm parametreler belirtilerek ripple oluşturur.
     */
    public void spawnRipple(float x, float z, float strength, float speed, float maxRadius) {
        if (ripples.size() >= MAX_RIPPLES) {
            ripples.remove(0); // En eskiyi çıkar
        }
        ripples.add(new WaterRipple(x, z, strength, speed, maxRadius));
    }

    // ==================== Shader Verisi ====================

    /**
     * Shader'a gönderilecek ripple merkezi verilerini float array olarak döndürür.
     * Format: [x0, z0, 0,  x1, z1, 0, ... ] (vec3 array → 3 float per ripple)
     * Boş slotlar 0 ile doldurulur.
     *
     * @return MAX_RIPPLES * 3 boyutunda float[]
     */
    public float[] getRippleCenters() {
        float[] data = new float[MAX_RIPPLES * 3];
        for (int i = 0; i < ripples.size() && i < MAX_RIPPLES; i++) {
            WaterRipple r = ripples.get(i);
            data[i * 3]     = r.getCenterX();
            data[i * 3 + 1] = 0f;             // Y placeholder (shader görmez)
            data[i * 3 + 2] = r.getCenterZ();
        }
        return data;
    }

    /**
     * Shader'a gönderilecek ripple parametre verilerini float array olarak döndürür.
     * Format: [radius0, strength0, age0,  radius1, strength1, age1, ... ]
     *
     * @return MAX_RIPPLES * 3 boyutunda float[]
     */
    public float[] getRippleData() {
        float[] data = new float[MAX_RIPPLES * 3];
        for (int i = 0; i < ripples.size() && i < MAX_RIPPLES; i++) {
            WaterRipple r = ripples.get(i);
            data[i * 3]     = r.getRadius();
            data[i * 3 + 1] = r.getEffectiveStrength();
            data[i * 3 + 2] = r.getAge();
        }
        return data;
    }

    /** @return Aktif ripple sayısı (shader'a int uniform olarak gönderilir) */
    public int getRippleCount() {
        return Math.min(ripples.size(), MAX_RIPPLES);
    }

    // ==================== Ayarlar ====================

    public float getDefaultStrength()          { return defaultStrength;  }
    public void  setDefaultStrength(float s)   { this.defaultStrength = s; }

    public float getDefaultSpeed()             { return defaultSpeed;     }
    public void  setDefaultSpeed(float s)      { this.defaultSpeed = s;   }

    public float getDefaultMaxRadius()         { return defaultMaxRadius; }
    public void  setDefaultMaxRadius(float r)  { this.defaultMaxRadius = r; }
}
