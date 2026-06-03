package environment;

import org.lwjgl.util.vector.Vector3f;
import scene.Scene;
import skybox.atmosphere.AtmosphereSky;

/**
 * Oyun iÃ§i gece-gÃ¼ndÃ¼z dÃ¶ngÃ¼sÃ¼nÃ¼ yÃ¶neten sistem.
 * GÃ¼neÅŸin hareketini, ortam aydÄ±nlatmasÄ±nÄ± ve gÃ¶kyÃ¼zÃ¼ renklerini senkronize eder.
 */
public class DayNightManager {

    private Scene scene;
    
    // Zaman 0.0 (Gece YarÄ±sÄ±) ile 24.0 arasÄ±nda akar. (Ã–rn: 12.0 = Ã–ÄŸlen, 18.0 = GÃ¼n BatÄ±mÄ±)
    private float timeOfDay;
    // ZamanÄ±n ne kadar hÄ±zlÄ± akacaÄŸÄ±nÄ± belirler (Ã–rn: 1.0 = GerÃ§ek zamanlÄ± saat gibi, 100.0 = Ã‡ok hÄ±zlÄ±)
    private float timeMultiplier;

    // --- Renk Paletleri ---
    private Vector3f dayLightColor = new Vector3f(1.0f, 0.95f, 0.9f); // GÃ¼ndÃ¼z GÃ¼neÅŸ Rengi
    private Vector3f sunsetLightColor = new Vector3f(1.0f, 0.5f, 0.2f); // GÃ¼n BatÄ±mÄ±/DoÄŸumu IÅŸÄ±ÄŸÄ±
    private Vector3f nightLightColor = new Vector3f(0.3f, 0.4f, 0.55f); // Gece AyÄ±ÅŸÄ±ÄŸÄ± (YÄ±ldÄ±z aydÄ±nlatmasÄ± eklendi)
    
    // --- Sis (Fog) Paletleri ---
    private Vector3f dayFogColor = new Vector3f(0.72f, 0.82f, 0.92f);
    private Vector3f sunsetFogColor = new Vector3f(0.7f, 0.4f, 0.3f);
    private Vector3f nightFogColor = new Vector3f(0.02f, 0.02f, 0.04f); // Gece karanlÄ±ÄŸÄ± sis
    
    private boolean planetaryMode = false;
    private Vector3f planetPosition = new Vector3f();

    private float sunOrbitYaw = 20.0f; // Güneşin doğuş-batış ekseninin rotasyonu (derece)


    public DayNightManager(Scene scene, float initialTimeOfDay, float timeMultiplier) {
        this.scene = scene;
        this.timeOfDay = initialTimeOfDay;
        this.timeMultiplier = timeMultiplier;
    }

    /**
     * Her karede (frame) zamanÄ± ileri sarÄ±p gÃ¼neÅŸin ve Ä±ÅŸÄ±klarÄ±n durumunu gÃ¼nceller.
     * @param delta Zaman farkÄ± (saniye)
     */
    public void update(float delta) {
        // ZamanÄ± ilerlet
        timeOfDay += (delta * timeMultiplier) / 3600f; // 1 saatlik gerÃ§ek zaman = 3600 saniye
        if (timeOfDay >= 24.0f) {
            timeOfDay -= 24.0f;
        }

        // --- GÃœNEÅž AÃ‡ISINI HESAPLA ---
        // Saat 06:00 (6.0) -> Ufuk (0 derece)
        // Saat 12:00 (12.0) -> Tepe (90 derece)
        // Saat 18:00 (18.0) -> Ufuk (180 derece)
        // Saat 00:00 (24.0) -> Ayak ucu (270/-90 derece)
        
        float timeRatio = (timeOfDay - 6.0f) / 24.0f; 
        float sunAngle = timeRatio * (float)Math.PI * 2f; 

        // GÃ¼neÅŸ YÃ¶nÃ¼ VektÃ¶rÃ¼ (Direction)
        Vector3f sunDirection;
        float sunHeight;
        
        if (planetaryMode) {
            // Gezegenin pozisyonundan GÃ¼neÅŸe (0,0,0) olan vektÃ¶r Ä±ÅŸÄ±k yÃ¶nÃ¼dÃ¼r.
            sunDirection = new Vector3f(-planetPosition.x, -planetPosition.y, -planetPosition.z);
            if (sunDirection.lengthSquared() > 0) {
                sunDirection.normalise();
            } else {
                sunDirection.set(1, 0, 0);
            }
            // Planetary modda 'gÃ¼neÅŸin yÃ¼ksekliÄŸi' kavramÄ± yerine,
            // gezegen kendi etrafÄ±nda dÃ¶nerek gece gÃ¼ndÃ¼zÃ¼ yaratÄ±r.
            // Fakat basitlik iÃ§in Ä±ÅŸÄ±k ÅŸiddetini hep sabit (gÃ¼ndÃ¼z) tutalÄ±m, 
            // Ã§Ã¼nkÃ¼ kÃ¼renin arkasÄ± zaten karanlÄ±k olacak (Lambertian lighting).
            sunHeight = 1.0f; // Hep tam aydÄ±nlÄ±k, gÃ¶lgede kalan yerler kendi kararÄ±r.
        } else {
            float sunDirY = (float)Math.sin(sunAngle);
            float baseDirX = (float)Math.cos(sunAngle);
            float baseDirZ = 0.0f; // Temel yörünge düzlemi

            // Güneş yörüngesini Y ekseni etrafında döndür (Compass rotation / Yaw)
            float yawRad = (float)Math.toRadians(sunOrbitYaw);
            float rotatedX = baseDirX * (float)Math.cos(yawRad) - baseDirZ * (float)Math.sin(yawRad);
            float rotatedZ = baseDirX * (float)Math.sin(yawRad) + baseDirZ * (float)Math.cos(yawRad);

            sunDirection = new Vector3f(rotatedX, sunDirY, rotatedZ);
            if (sunDirection.lengthSquared() > 0) {
                sunDirection.normalise();
            }
            sunHeight = sunDirY;
        }

        // --- SCENE IŞIK YÖNÜNÜ GÜNCELLE ---
        // Işığın "geliş" yönü güneşin pozisyonunun tam tersidir (-sunDirection).
        Vector3f lightDirection = new Vector3f(-sunDirection.x, -sunDirection.y, -sunDirection.z);
        scene.setLightDirection(lightDirection);
        if (scene.getSun() != null) {
            scene.getSun().setDirection(lightDirection);
        }

        // --- IÅžIK RENGÄ° VE PARLAKLIÄžI (BLENDING) ---

        Vector3f currentLightColor = new Vector3f();
        Vector3f currentFogColor = new Vector3f();
        float currentBrightness;
        float currentAmbient;

        if (sunHeight > 0.1f) {
            // GÃ¼ndÃ¼z
            float blendFactor = Math.min((sunHeight - 0.1f) / 0.3f, 1.0f); // 0.1 ile 0.4 arasÄ±nda kÄ±zÄ±ldan beyaza dÃ¶n
            Vector3f.sub(dayLightColor, sunsetLightColor, currentLightColor);
            currentLightColor.scale(blendFactor);
            Vector3f.add(sunsetLightColor, currentLightColor, currentLightColor);
            
            Vector3f.sub(dayFogColor, sunsetFogColor, currentFogColor);
            currentFogColor.scale(blendFactor);
            Vector3f.add(sunsetFogColor, currentFogColor, currentFogColor);
            
            currentBrightness = 0.8f + (blendFactor * 0.2f); // Ã–ÄŸlen en parlak
            currentAmbient = 0.4f + (blendFactor * 0.2f);
        } else if (sunHeight > -0.1f) {
            // GÃ¼n BatÄ±mÄ± / DoÄŸumu (-0.1 ile 0.1 arasÄ±)
            float blendFactor = (sunHeight + 0.1f) / 0.2f; // 0'dan 1'e
            Vector3f.sub(sunsetLightColor, nightLightColor, currentLightColor);
            currentLightColor.scale(blendFactor);
            Vector3f.add(nightLightColor, currentLightColor, currentLightColor);
            
            Vector3f.sub(sunsetFogColor, nightFogColor, currentFogColor);
            currentFogColor.scale(blendFactor);
            Vector3f.add(nightFogColor, currentFogColor, currentFogColor);
            
            currentBrightness = 0.5f + (blendFactor * 0.3f);
            currentAmbient = 0.3f + (blendFactor * 0.1f);
        } else {
            // Gece
            currentLightColor.set(nightLightColor);
            currentFogColor.set(nightFogColor);
            currentBrightness = 0.5f; // AyÄ±ÅŸÄ±ÄŸÄ± parlaklÄ±ÄŸÄ± artÄ±rÄ±ldÄ±
            currentAmbient = 0.3f; // KaranlÄ±k seviyesi starlight'a gÃ¶re ayarlandÄ±
        }

        scene.setLightColor(currentLightColor);
        scene.setLightBrightness(currentBrightness);
        scene.setAmbientLight(currentAmbient);
        scene.setFogColor(currentFogColor);

        // --- ATMOSPHERESKY ENTEGRASYONU ---
        // EÄŸer gÃ¶kyÃ¼zÃ¼ AtmosphereSky ise, gÃ¼neÅŸ objesinin koordinatÄ±nÄ± (sunPosition) bildirelim.
        if (scene.getSky() != null && scene.getSky() instanceof AtmosphereSky) {
            AtmosphereSky atmo = (AtmosphereSky) scene.getSky();
            // AtmosphereSky gÃ¼neÅŸin pozisyonunu (yÃ¶nÃ¼nÃ¼) bekliyor.
            // 1000f uzaklÄ±kta dev bir kÃ¼re gibi dÃ¼ÅŸÃ¼nelim.
            Vector3f sunPos = new Vector3f(sunDirection.x * 1000f, sunDirection.y * 1000f, sunDirection.z * 1000f);
            atmo.setSunPosition(sunPos);
        }
    }

    public float getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(float timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    public float getTimeMultiplier() {
        return timeMultiplier;
    }

    public void setTimeMultiplier(float timeMultiplier) {
        this.timeMultiplier = timeMultiplier;
    }

    public void setPlanetaryMode(boolean planetaryMode) {
        this.planetaryMode = planetaryMode;
    }
    
    public void setPlanetPosition(Vector3f pos) {
        this.planetPosition.set(pos);
    }

    public float getSunOrbitYaw() {
        return sunOrbitYaw;
    }

    public void setSunOrbitYaw(float sunOrbitYaw) {
        this.sunOrbitYaw = sunOrbitYaw;
    }
}

