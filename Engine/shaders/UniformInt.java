package shaders;

import org.lwjgl.opengl.GL20;

/**
 * Shader kodundaki bir 'int' uniform değişkeni temsil eder.
 * Ripple sayacı (rippleCount) gibi tam sayı uniform'ları için kullanılır.
 */
public class UniformInt extends Uniform {

    private int currentValue;
    private boolean used = false;

    /**
     * @param name Shader içindeki int uniform adı
     */
    public UniformInt(String name) {
        super(name);
    }

    /**
     * GPU'daki shader'a yeni int değerini yükler.
     * Değer değişmemişse yükleme pas geçilir (performans optimizasyonu).
     *
     * @param value Yüklenecek tam sayı değer
     */
    public void loadInt(int value) {
        if (!used || currentValue != value) {
            GL20.glUniform1i(super.getLocation(), value);
            used = true;
            currentValue = value;
        }
    }
}
