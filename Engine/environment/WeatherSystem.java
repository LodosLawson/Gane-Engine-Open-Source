package environment;

import org.lwjgl.util.vector.Vector3f;
import scene.Scene;
import skybox.atmosphere.AtmosphereSky;

/**
 * Gane Engine iÃ§in dinamik hava durumu ve bulut dÃ¶ngÃ¼lerini yÃ¶neten sÄ±nÄ±f.
 * Zaman tabanlÄ± (periyodik) hava durumu deÄŸiÅŸimlerini (aÃ§Ä±k gÃ¶kyÃ¼zÃ¼, parÃ§alÄ± bulutlu, kapalÄ±) simÃ¼le eder.
 * GeliÅŸtirici, Ã§alÄ±ÅŸma zamanÄ±nda (runtime) tÃ¼m hava durumu detaylarÄ±na eriÅŸebilir ve deÄŸiÅŸtirebilir.
 */
public class WeatherSystem {

    private final Scene scene;

    // Periyodik hava durumu dÃ¶ngÃ¼sÃ¼ ayarlarÄ±
    private float cycleDuration = 600f; // VarsayÄ±lan dÃ¶ngÃ¼ sÃ¼resi (saniye cinsinden, Ã¶rn: 10 dakika)
    private float cycleTime = 0f;       // DÃ¶ngÃ¼deki mevcut zaman

    // Manuel kontrol/override ayarlarÄ±
    private boolean overrideEnabled = false;
    private float overrideCoverage = 0.0f; // 0.0 (aÃ§Ä±k) ile 1.0 (kapalÄ±) arasÄ± manuel bulutluluk oranÄ±

    // Aktif hesaplanan bulutluluk oranÄ±
    private float currentCoverage = 0.0f;

    // 3 KatmanlÄ± bulut yÃ¼kseklik sÄ±nÄ±rlarÄ±
    private float[] layerMinHeights = { 3000f, 4600f, 6800f };
    private float[] layerMaxHeights = { 3800f, 5400f, 7600f };

    // RÃ¼zgar ve bulut yoÄŸunluk Ã§arpanlarÄ±
    private float windSpeedMultiplier = 0.25f; // Saniyede majestik ve yavaÅŸ sÃ¼zÃ¼lme iÃ§in varsayÄ±lan azaltÄ±ldÄ±
    private float cloudDensityMultiplier = 1.0f;

    // Dinamik rÃ¼zgar deÄŸiÅŸkenleri
    private Vector3f currentWind = new Vector3f(5f, 0f, 2f);
    private Vector3f targetWind = new Vector3f(5f, 0f, 2f);
    private float windChangeTimer = 0f;
    private float windChangeInterval = 10f; // 10 saniyede bir yeni rÃ¼zgar hedefi

    // Manual rÃ¼zgar override
    private boolean manualWindOverride = false;

    public WeatherSystem(Scene scene) {
        this.scene = scene;
        if (scene != null) {
            currentWind.set(scene.getWindVelocity());
            targetWind.set(scene.getWindVelocity());
        }
    }

    /**
     * Her frame'de hava durumu zaman dÃ¶ngÃ¼sÃ¼nÃ¼ gÃ¼nceller ve bulut parametrelerini hesaplar.
     * @param delta GeÃ§en sÃ¼re (saniye)
     */
    public void update(float delta) {
        if (overrideEnabled) {
            currentCoverage = Math.max(0.0f, Math.min(1.0f, overrideCoverage));
        } else {
            cycleTime += delta;
            if (cycleTime >= cycleDuration) {
                cycleTime -= cycleDuration;
            }
            // SinÃ¼s dalgasÄ± ile yumuÅŸak geÃ§iÅŸler:
            // raw deÄŸeri -1.0 ile 1.0 arasÄ±nda deÄŸiÅŸir.
            // FormÃ¼l: raw * 1.5 + 0.5 -> -1.0 ile 2.0 arasÄ±nda sonuÃ§ verir.
            // Bu sonuÃ§ [0.0, 1.0] arasÄ±na clamp edilir.
            // BÃ¶ylece dÃ¶ngÃ¼nÃ¼n yaklaÅŸÄ±k %35'inde gÃ¶kyÃ¼zÃ¼ tamamen aÃ§Ä±k (0.0),
            // diÄŸer %35'inde tamamen kapalÄ± (1.0), ve kalan %30'unda geÃ§iÅŸ halindedir.
            float raw = (float) Math.sin(cycleTime * 2.0 * Math.PI / cycleDuration);
            currentCoverage = Math.max(0.0f, Math.min(1.0f, raw * 1.5f + 0.5f));
        }

        // Dinamik RÃ¼zgar HesaplamasÄ±
        if (!manualWindOverride) {
            windChangeTimer += delta;
            if (windChangeTimer >= windChangeInterval) {
                windChangeTimer = 0f;
                // Rastgele yeni rÃ¼zgar yÃ¶nÃ¼ ve ÅŸiddeti belirle
                float randomSpeed = (float) (Math.random() * 25.0f); // 0 ile 25 arasÄ± rÃ¼zgar
                float randomAngle = (float) (Math.random() * Math.PI * 2.0);
                targetWind.x = (float) Math.cos(randomAngle) * randomSpeed;
                targetWind.z = (float) Math.sin(randomAngle) * randomSpeed;
            }
        }
        
        // Mevcut rÃ¼zgarÄ± hedefe doÄŸru yumuÅŸakÃ§a (interpolate) kaydÄ±r
        float lerpFactor = delta * 0.5f; // YumuÅŸak geÃ§iÅŸ hÄ±zÄ±
        currentWind.x += (targetWind.x - currentWind.x) * lerpFactor;
        currentWind.z += (targetWind.z - currentWind.z) * lerpFactor;
        
        if (scene != null) {
            scene.setWindVelocity(currentWind);
        }

        // AtmosphereSky varsa parametreleri gÃ¼ncelle ve rÃ¼zgarÄ± senkronize et
        if (scene != null && scene.getSky() instanceof AtmosphereSky) {
            AtmosphereSky sky = (AtmosphereSky) scene.getSky();
            sky.setWind(scene.getWindVelocity());
            
            // Bulut yoÄŸunluÄŸu rÃ¼zgara gÃ¶re de ÅŸekillensin: Ã§ok rÃ¼zgar = daha gri/kapanÄ±k hava potansiyeli
            float windMag = currentWind.length();
            float windEffect = Math.min(1.0f, windMag / 20.0f);
            
            // Override yoksa bulutlarÄ± rÃ¼zgarla birleÅŸtir
            if (!overrideEnabled) {
                sky.setCloudDensityMultiplier(cloudDensityMultiplier + windEffect * 0.5f);
                sky.setWindSpeedMultiplier(windSpeedMultiplier + windEffect * 0.5f);
            } else {
                sky.setWindSpeedMultiplier(windSpeedMultiplier);
                sky.setCloudDensityMultiplier(cloudDensityMultiplier);
            }
        }
    }

    public void forceWindTarget(float vx, float vz) {
        this.manualWindOverride = true;
        this.targetWind.set(vx, 0f, vz);
    }
    
    public void resumeDynamicWind() {
        this.manualWindOverride = false;
    }
    
    public Vector3f getCurrentWind() {
        return currentWind;
    }

    // --- GELÄ°ÅžTÄ°RÄ°CÄ° API'SÄ° (GETTER / SETTER) ---

    public float getCycleDuration() {
        return cycleDuration;
    }

    public void setCycleDuration(float cycleDuration) {
        this.cycleDuration = cycleDuration;
    }

    public float getCycleTime() {
        return cycleTime;
    }

    public void setCycleTime(float cycleTime) {
        this.cycleTime = cycleTime;
    }

    public boolean isOverrideEnabled() {
        return overrideEnabled;
    }

    public void setOverrideEnabled(boolean overrideEnabled) {
        this.overrideEnabled = overrideEnabled;
    }

    public float getOverrideCoverage() {
        return overrideCoverage;
    }

    public void setOverrideCoverage(float overrideCoverage) {
        this.overrideCoverage = overrideCoverage;
    }

    public float getCurrentCoverage() {
        return currentCoverage;
    }

    public void setCurrentCoverage(float coverage) {
        this.overrideEnabled = true;
        this.overrideCoverage = coverage;
        this.currentCoverage = coverage;
    }

    public float getLayerMinHeight(int layer) {
        if (layer >= 0 && layer < 3) {
            return layerMinHeights[layer];
        }
        return 3000f;
    }

    public void setLayerMinHeight(int layer, float val) {
        if (layer >= 0 && layer < 3) {
            layerMinHeights[layer] = val;
        }
    }

    public float getLayerMaxHeight(int layer) {
        if (layer >= 0 && layer < 3) {
            return layerMaxHeights[layer];
        }
        return 3800f;
    }

    public void setLayerMaxHeight(int layer, float val) {
        if (layer >= 0 && layer < 3) {
            layerMaxHeights[layer] = val;
        }
    }

    public float getWindSpeedMultiplier() {
        return windSpeedMultiplier;
    }

    public void setWindSpeedMultiplier(float windSpeedMultiplier) {
        this.windSpeedMultiplier = windSpeedMultiplier;
    }

    public float getCloudDensityMultiplier() {
        return cloudDensityMultiplier;
    }

    public void setCloudDensityMultiplier(float cloudDensityMultiplier) {
        this.cloudDensityMultiplier = cloudDensityMultiplier;
    }
}

