package water;

/**
 * Bir objenin suya çarpmasıyla oluşan ve zamanla genişleyip sönen
 * su yüzeyi dalga halkasını (ripple) temsil eder.
 *
 * <p>Her ripple'ın dünya koordinatındaki merkezi, gücü, yayılma hızı
 * ve ömrü vardır. Shader'a bu veriler array halinde gönderilir ve
 * vertex shader su yüzeyini buna göre büker.</p>
 */
public class WaterRipple {

    /** Aynı anda şaderda işlenebilecek maksimum ripple sayısı (GLSL array limiti) */
    public static final int MAX_RIPPLES = 16;

    private float centerX;       // Dalga merkezinin dünya X koordinatı
    private float centerZ;       // Dalga merkezinin dünya Z koordinatı
    private float strength;      // Maksimum dalga genliği (vertex displacement)
    private float speed;         // Dalganın yayılma hızı (birim/saniye)
    private float radius;        // Anlık yayılma yarıçapı (zamanla büyür)
    private float maxRadius;     // Dalga bu yarıçapa ulaşınca tamamen solar
    private float age;           // Yaş [0.0 → 1.0] (1.0 = tamamen soldu)

    /**
     * Yeni bir ripple oluşturur.
     *
     * @param centerX  Dünya X koordinatı
     * @param centerZ  Dünya Z koordinatı
     * @param strength Maksimum dalga genliği (0.1–0.8 arası önerilir)
     * @param speed    Yayılma hızı (birim/saniye, örn: 8.0f)
     * @param maxRadius Dalganın erişeceği maksimum yarıçap
     */
    public WaterRipple(float centerX, float centerZ, float strength, float speed, float maxRadius) {
        this.centerX   = centerX;
        this.centerZ   = centerZ;
        this.strength  = strength;
        this.speed     = speed;
        this.maxRadius = maxRadius;
        this.radius    = 0.1f;  // Sıfır bölme hatasını önlemek için küçük başlangıç
        this.age       = 0f;
    }

    /**
     * Her karede çağrılır. Yaşı ve yarıçapı artırır; dalga zamanla sönümlenir.
     *
     * @param delta Bu karenin geçen süresi (saniye)
     */
    public void update(float delta) {
        radius += speed * delta;
        age = Math.min(radius / maxRadius, 1f);  // Yarıçap → ömür eşlemesi
    }

    /** @return Bu ripple artık görünmez durumdaysa true (yaş 1.0'a ulaştı) */
    public boolean isDead() {
        return age >= 1f;
    }

    // ==================== Getters ====================

    public float getCenterX()  { return centerX;  }
    public float getCenterZ()  { return centerZ;  }
    public float getRadius()   { return radius;   }
    public float getAge()      { return age;       }

    /**
     * Anlık efektif gücü hesaplar: yaş arttıkça güç üstel olarak düşer.
     * Bu sayede yeni dalga güçlü, yaşlı dalga zarif biçimde solar.
     */
    public float getEffectiveStrength() {
        float fade = (float) Math.pow(1f - age, 1.8f);
        return strength * fade;
    }
}
