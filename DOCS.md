# 🎮 Gane Engine — Geliştirici Referans Kılavuzu

> **Sürüm:** 1.1.0
> **Teknoloji:** Java 8+ / LWJGL 2.9.3 / OpenGL 3.3+  
> **Mimari:** Forward Rendering + Shadow Mapping + FFT Ocean + Gane IDE

---

## 📁 Proje Yapısı

```
gane/
├── Engine/                  ← Motor çekirdeği (tüm alt sistemler)
│   ├── renderEngine/        ← Ana render döngüsü
│   ├── scene/               ← Sahne, Entity, Model, Skin, Light
│   ├── terrain/             ← Arazi sistemleri (Flat, Voxel, Planet)
│   ├── water/               ← Su yüzeyi ve FFT Okyanus
│   ├── skybox/              ← Gökyüzü, Atmosfer, Bulutlar
│   ├── environment/         ← Gece/Gündüz, Hava Durumu
│   ├── physics/             ← Fizik motoru, yerçekimi, çarpışma
│   ├── particles/           ← Partikül sistemi
│   ├── shadows/             ← Gölge haritası (Shadow Mapping)
│   ├── entityRenderers/     ← Nesne çizim motorları
│   ├── postProcessing/      ← Bloom, Blur, Kontrast
│   ├── guiRendering/        ← 2D UI sistemi
│   ├── utils/               ← Kamera, Ses, Ekran, MousePicker
│   ├── loaders/             ← OBJ/Scene yükleyiciler
│   ├── textures/            ← Doku yükleme ve yönetim
│   ├── lensFlare/           ← Güneş lens parlaması
│   ├── sunRenderer/         ← Güneş çizimi
│   ├── shinyRenderer/       ← Yansıtıcı nesneler
│   ├── objects/             ← Hazır nesneler (Tree3D, Grass3D, WLL)
│   ├── network/             ← Multiplayer (Client/Server/Steam P2P)
│   ├── steam/               ← Steamworks entegrasyonu
│   ├── shaders/             ← GLSL shader programı altyapısı
│   └── openglObjects/       ← VAO/VBO soyutlaması
│
├── src/gane/                ← Oyun katmanı (uygulamaya özel)
│   ├── MainApp.java         ← Ana giriş noktası ve oyun döngüsü
│   ├── CinematicManager.java← Sinematik demo yöneticisi
│   ├── SplashScreen.java    ← Yükleme ekranı
│   ├── Menu/                ← Menü sistemleri
│   └── objects/             ← Oyuna özel nesneler (Player)
│
├── Resources/               ← Ses, doku, model dosyaları
├── lwjgl-2.9.3/             ← LWJGL bağımlılıkları
└── lib/                     ← Ek kütüphaneler
```

---

## 1. 🖥️ Render Engine — `renderEngine`

Motor çekirdeği. Pencere oluşturma, tüm alt render sistemlerini orkestre etme.

### `RenderEngine`
Ana render motoru. `init()` ile başlatılır, oyun döngüsünde `renderScene()` çağrılır.

| Metot | Açıklama |
|-------|----------|
| `static RenderEngine init()` | Motoru başlatır (pencere, renderer'lar). **Sadece 1 kez çağrılır.** |
| `void renderScene(Scene scene, float delta)` | Sahneyi ekrana çizer |
| `void renderEnvironmentMap(Texture, Scene, Vector3f)` | Dinamik küp haritası çizer (parlak yansımalar için) |
| `void update()` | Frame sonunda ekranı günceller (buffer swap) |
| `MasterRenderer getMasterRenderer()` | Alt render yöneticisine erişim |
| `DisplayManager getDisplayManager()` | Ekran/pencere yöneticisine erişim |
| `void close()` | Motoru ve pencereyi kapatır |

```java
// Kullanım
RenderEngine renderEngine = RenderEngine.init();

while (!Display.isCloseRequested()) {
    float delta = renderEngine.getDisplayManager().getFrameTime();
    renderEngine.renderScene(scene, delta);
    renderEngine.update();
}

renderEngine.close();
```

---

## 2. 🎬 Scene (Sahne) — `scene`

Oyun dünyasındaki her şeyi barındıran merkez.

### `Scene`
Kamerayı, gökyüzünü, ışıkları, nesneleri, su yüzeylerini ve arazileri yönetir.

| Metot | Açıklama |
|-------|----------|
| `Scene(ICamera, Skybox)` | Varsayılan sahne oluşturur |
| `Scene(ICamera, ISky, boolean renderWater)` | Su açık/kapalı seçenekli |
| `void addEntity(Entity)` | Sahneye nesne ekler |
| `void removeEntity(Entity)` | Nesnеyi kaldırır |
| `void addTerrain(ITerrain)` | Arazi ekler (FlatTerrain/VoxelTerrain) |
| `void addWater(WaterTile)` | Su yüzeyi ekler |
| `void addShiny(Entity)` | Yansıtıcı nesne ekler |
| `void addPointLight(Light)` | Nokta ışık ekler |
| `void clearPointLights()` | Tüm nokta ışıkları siler |
| `void setLightDirection(Vector3f)` | Güneş yönünü ayarlar |
| `void setLightColor(Vector3f)` | Güneş rengini ayarlar |
| `void setLightBrightness(float)` | Güneş parlaklığı (0-1) |
| `void setAmbientLight(float)` | Ortam ışığı (gölge minimum) |
| `void setSky(ISky)` | Gökyüzünü değiştirir |
| `void setWindVelocity(Vector3f)` | Global rüzgar yönü/hızı |
| `void setFogColor(Vector3f)` | Sis rengi |
| `void setFogDensity(float)` | Sis yoğunluğu (0-1) |
| `void setFogStart(float)` | Sisin başladığı mesafe |
| `void setFrustumCullingEnabled(boolean)` | Frustum culling açma/kapama |
| `void setLodEnabled(boolean)` | Level of Detail açma/kapama |
| `void setGrassField(GrassField)` | Çimen alanı atar |
| `void delete()` | Sahneyi tamamen siler |

```java
// Sahne oluşturma
Scene scene = new Scene(camera, atmosphereSky, true);
scene.setLightDirection(new Vector3f(0.3f, -1f, 0.5f));
scene.setAmbientLight(0.4f);
scene.setFogDensity(0.5f);
scene.setFogStart(2000f);
```

---

### `Entity`
Sahnedeki her 3D nesne.

| Metot | Açıklama |
|-------|----------|
| `Entity(Model, Skin)` | Model + materyal ile nesne oluşturur |
| `Vector3f getPosition()` | Konum (x, y, z) |
| `void setPosition(Vector3f)` | Konumu atar |
| `Vector3f getRotation()` | Dönme açıları (derece) |
| `void setRotation(Vector3f)` | Dönme açılarını atar |
| `float getScale()` | Boyut çarpanı |
| `void setScale(float)` | Boyutu ayarlar |
| `void setCastsShadow(boolean)` | Gölge üretimi açma/kapama |
| `void setHasReflection(boolean)` | Suda yansıma |
| `void setSeenUnderWater(boolean)` | Su altında görünürlük |
| `void setImportant(boolean)` | Düşük kalitede bile çizilir |
| `void setLodModels(Model, Model)` | LOD1 ve LOD2 modelleri |
| `void setLodDistances(float, float)` | LOD geçiş mesafeleri |
| `void setCullingRadius(float)` | Frustum culling yarıçapı |
| `void setPhysicsComponent(PhysicsComponent)` | Fizik bileşeni atar |
| `void setBoundingBox(AABB)` | Çarpışma kutusu |
| `void setTextureIndex(int)` | Doku atlas indeksi |
| `void update(float delta)` | Override ile özel davranış |
| `void delete()` | GPU belleğinden siler |

```java
Entity tree = new Tree3D();
tree.getPosition().set(100, 0, 200);
tree.setScale(2.0f);
tree.setCastsShadow(true);
scene.addEntity(tree);
```

---

### `Model`
GPU belleğindeki geometri verisi (VAO sarmalayıcı).

| Metot | Açıklama |
|-------|----------|
| `Model(Vao)` | VAO'dan model oluşturur |
| `Vao getVao()` | VAO'ya erişim |
| `void delete()` | GPU belleğinden siler |

---

### `Skin`
Nesnenin yüzey materyali (doku, saydamlık, ışıklandırma).

| Metot | Açıklama |
|-------|----------|
| `Skin(Texture diffuse, Texture extraInfo)` | Dokulu materyal |
| `void setTransparent(boolean)` | Saydamlık (yaprak, cam) |
| `void setUseFakeLighting(boolean)` | Sahte ışıklandırma (çimen) |
| `void setCullBackFaces(boolean)` | Arka yüz gizleme |
| `void setNumberOfRows(int)` | Doku atlas boyutu |
| `void delete()` | Dokuları siler |

---

### `Light`
Nokta ışık kaynağı (meşale, lamba, ateş).

| Metot | Açıklama |
|-------|----------|
| `Light(Vector3f pos, Vector3f color)` | Sönümsüz ışık (güneş tipi) |
| `Light(Vector3f pos, Vector3f color, Vector3f attenuation)` | Sönümlü nokta ışık |
| `void setPosition(Vector3f)` | Konumu değiştirir |
| `void setColor(Vector3f)` | Rengi değiştirir |
| `void setAttenuation(Vector3f)` | Sönümleme (sabit, doğrusal, kare) |

> **Attenuation (Sönümleme):** `x` = sabit, `y` = doğrusal, `z` = kare mesafe azalması.

```java
// Meşale (sarı-turuncu, 50 birim menzil)
Light torch = new Light(
    new Vector3f(10, 5, 10),           // Konum
    new Vector3f(100f, 80f, 20f),      // Renk (R, G, B)
    new Vector3f(1, 0.005f, 0.0005f)   // Sönümleme
);
scene.addPointLight(torch);
```

---

## 3. 🏔️ Terrain (Arazi) — `terrain`

### `ITerrain` (Arayüz)
Tüm arazi tiplerinin uyguladığı temel arayüz.

| Metot | Açıklama |
|-------|----------|
| `float getHeightAt(float x, float z)` | Verilen koordinattaki yüzey yüksekliği |
| `void render(ICamera, Scene, Matrix4f, int)` | Araziyi çizer |
| `float getWidth()` / `getDepth()` | Arazi boyutları |
| `void setClipPlane(Vector4f)` | Su yansıması için kırpma düzlemi |
| `void cleanUp()` | GPU belleğinden temizler |

---

### `FlatTerrain`
Yükseklik haritası tabanlı düz arazi. Gölgeleme, çoklu doku, çimen, ağaç desteği.

```java
FlatTerrain terrain = new FlatTerrain(512, 512);
terrain.setHighQuality(true);
terrain.regenerate();
scene.addTerrain(terrain);
```

---

### `VoxelTerrain`
Minecraft tarzı küp (voxel) tabanlı arazi. Dinamik kazma/inşa desteği.

```java
VoxelTerrain voxel = new VoxelTerrain();
scene.addTerrain(voxel);
```

---

### `PlanetTerrain`
Küresel gezegen arazisi. Gezegensel yerçekimi ile uyumlu.

---

### `FloraManager`
Arazi üzerinde otomatik ağaç ve bitki yerleştirme sistemi.

---

### `GrassField`
Rüzgarda sallanan çimen (GPU instancing).

---

### `HeightsGenerator`
Prosedürel yükseklik haritası oluşturucu (Perlin Noise vb.).

---

## 4. 🌊 Water (Su) — `water`

### `WaterTile`
Okyanus / göl su yüzeyi. Dalga, köpük, renk, saydamlık her şey ayarlanabilir.

| Metot | Açıklama |
|-------|----------|
| `WaterTile(float x, float z, float height, float size)` | Su yüzeyi oluşturur |
| `void setBaseColor(float r, float g, float b)` | Yüzey rengi |
| `void setDeepColor(float r, float g, float b)` | Derin su rengi |
| `void setTransparency(float)` | Saydamlık (0-1) |
| `void setWaveHeight(float)` | Dalga yüksekliği |
| `void setWindSpeed(float)` | Rüzgar hızı (m/s) |
| `void setWindDirection(float x, float z)` | Rüzgar yönü |
| `void setTimeScale(float)` | Dalga animasyon hızı |
| `void setSpecularPower(float)` | Güneş yansıması keskinliği |
| `void setSpecularIntensity(float)` | Güneş yansıması gücü |
| `void setFresnelPower(float)` | Kenar parlaması |
| `void setFoamIntensity(float)` | Köpük yoğunluğu (0-1) |
| `void setFoamThreshold(float)` | Köpük eşiği |
| `void setUnderwaterFogDensity(float)` | Su altı sis yoğunluğu |
| `void setUnderwaterFogColor(float r, float g, float b)` | Su altı sis rengi |
| `float getWaterHeightAt(float x, float z)` | Verilen koordinattaki su yüksekliği |

```java
WaterTile ocean = new WaterTile(0, 0, -2.0f, 5000f);
ocean.setBaseColor(0.01f, 0.22f, 0.36f);     // Koyu lacivert
ocean.setWaveHeight(1.5f);
ocean.setWindSpeed(8.0f);
ocean.setFoamIntensity(0.6f);
scene.addWater(ocean);
```

### `water.ocean` Alt Paketi
FFT (Hızlı Fourier Dönüşümü) tabanlı gerçekçi okyanus dalgaları.

| Sınıf | Açıklama |
|-------|----------|
| `OceanFFT` | GPU compute shader ile FFT dalga simülasyonu |
| `OceanMesh` | Okyanus mesh ızgarası |
| `OceanRenderer` | Okyanus çizim motoru |

---

## 5. 🌤️ Skybox (Gökyüzü) — `skybox`

### `ISky` (Arayüz)
Tüm gökyüzü tiplerinin temel arayüzü.

### `AtmosphereSky`
Fizik tabanlı atmosferik gökyüzü. Rayleigh/Mie saçılımı, dinamik bulutlar, güneş/ay.

| Metot | Açıklama |
|-------|----------|
| `void setSunPosition(Vector3f)` | Güneş konumunu ayarlar |
| `void setWind(Vector3f)` | Bulut hareketini etkileyen rüzgar |
| `void setCloudDensityMultiplier(float)` | Bulut yoğunluğu çarpanı |
| `void setWindSpeedMultiplier(float)` | Bulut hız çarpanı |
| `void setWeatherSystem(WeatherSystem)` | Hava durumu sistemini bağlar |

### `Skybox`
6-yüzlü küp harita (cubemap) gökyüzü (statik/hızlı).

### `CloudCluster`
Hacimsel/prosedürel bulut kümesi.

---

## 6. 🌙 Environment (Çevre) — `environment`

### `DayNightManager`
24 saatlik gece/gündüz döngüsü. Güneş hareketi, ışık rengi ve parlaklık otomasyonu.

| Metot | Açıklama |
|-------|----------|
| `DayNightManager(Scene, float startHour, float timeMultiplier)` | Oluşturur |
| `void update(float delta)` | Her frame günceller |
| `float getTimeOfDay()` | Mevcut saat (0-24) |
| `void setTimeOfDay(float)` | Saati atar |
| `float getTimeMultiplier()` | Zaman hızı çarpanı |
| `void setTimeMultiplier(float)` | Zaman hızını ayarlar |
| `void setPlanetaryMode(boolean)` | Gezegensel güneş modu |
| `void setPlanetPosition(Vector3f)` | Gezegen konumu |

> **Saat referansı:** `6.0` = Gün doğumu, `12.0` = Öğlen, `18.0` = Gün batımı, `0.0` = Gece yarısı

```java
DayNightManager dayNight = new DayNightManager(scene, 8.0f, 600f);
// Her frame:
dayNight.update(delta);
```

---

### `WeatherSystem`
Dinamik hava durumu ve bulut döngüleri.

| Metot | Açıklama |
|-------|----------|
| `void update(float delta)` | Her frame günceller |
| `void setCycleDuration(float)` | Hava döngüsü süresi (saniye) |
| `void setOverrideEnabled(boolean)` | Manuel kontrol |
| `void setOverrideCoverage(float)` | Manuel bulutluluk (0-1) |
| `void setCurrentCoverage(float)` | Anlık bulutluluk |
| `void setCloudDensityMultiplier(float)` | Bulut yoğunluk çarpanı |
| `void setWindSpeedMultiplier(float)` | Rüzgar hız çarpanı |
| `void forceWindTarget(float vx, float vz)` | Manuel rüzgar yönü |
| `void resumeDynamicWind()` | Otomatik rüzgara geri dön |
| `Vector3f getCurrentWind()` | Mevcut rüzgar vektörü |

```java
// Fırtına simülasyonu
WeatherSystem weather = scene.getWeatherSystem();
weather.setOverrideEnabled(true);
weather.setOverrideCoverage(0.9f);  // %90 bulutlu
weather.forceWindTarget(20f, 15f);  // Kuvvetli rüzgar
```

---

## 7. ⚡ Physics (Fizik) — `physics`

### `PhysicsEngine`
Yerçekimi, hız, çarpışma, su fiziği (buoyancy), rüzgar sürüklemesi.

| Metot | Açıklama |
|-------|----------|
| `void update(Scene, float delta)` | Tüm fiziksel objeleri günceller |
| `void setGlobalGravity(Vector3f)` | Yerçekimi vektörü |
| `void setAirDrag(float)` | Hava sürtünmesi (0-1) |
| `void setGravityMode(GravityMode)` | Yerçekimi modu |
| `void setPlanetaryCenter(Vector3f)` | Gezegensel çekim merkezi |
| `void setPlanetaryGravityStrength(float)` | Gezegensel çekim gücü |
| `void setWindVelocity(Vector3f)` | Rüzgar vektörü |

### `GravityMode` (Enum)

| Değer | Açıklama |
|-------|----------|
| `DIRECTIONAL` | Standart yönsel yerçekimi (Dünya) |
| `ZERO_GRAVITY` | Sıfır yerçekimi (Uzay boşluğu) |
| `PLANETARY` | Gezegensel çekim (Küresel, 1/r² azalma) |

```java
PhysicsEngine physics = new PhysicsEngine();

// Dünya fiziği
physics.setGravityMode(GravityMode.DIRECTIONAL);
physics.setGlobalGravity(new Vector3f(0, -9.81f, 0));

// Uzay fiziği
physics.setGravityMode(GravityMode.ZERO_GRAVITY);

// Gezegen fiziği
physics.setGravityMode(GravityMode.PLANETARY);
physics.setPlanetaryCenter(new Vector3f(0, 0, 0));
physics.setPlanetaryGravityStrength(9.81f);
```

### `PhysicsComponent`
Bir Entity'ye fizik davranışı ekler.

| Özellik | Açıklama |
|---------|----------|
| `velocity` | Hız vektörü |
| `acceleration` | İvme vektörü |
| `mass` | Kütle |
| `bounciness` | Sekme katsayısı (0-1) |
| `gravityScale` | Yerçekimi çarpanı |
| `isStatic` | Sabit nesne mi (duvar/zemin) |
| `is2D` | 2 boyutlu hareket |
| `canBeWaterLogged` | Su çekiyor mu |
| `volume` | Hacim (buoyancy hesabı) |

### `AABB`
Eksenlerle hizalı çarpışma kutusu (Axis-Aligned Bounding Box).

### `Collider` (Arayüz)
Çarpışma algılama temel arayüzü.

---

## 8. 📷 Camera (Kamera) — `extra`

### `Camera`
5 farklı mod destekleyen kamera sistemi.

| Metot | Açıklama |
|-------|----------|
| `Camera()` | Kamera oluşturur |
| `void setMode(CameraMode)` | Kamera modunu değiştirir |
| `CameraMode getMode()` | Mevcut mod |
| `void setTarget(Entity)` | Takip edilecek nesne |
| `void move()` | Her frame günceller |
| `Vector3f getPosition()` | Kamera konumu |
| `void setPitch(float)` | Yukarı/aşağı bakış açısı |
| `void setYaw(float)` | Sağ/sol bakış açısı |
| `float getPitch()` / `getYaw()` / `getRoll()` | Açı değerleri |
| `void reflect(float height)` | Su yansıması için ters çevir |
| `Matrix4f getViewMatrix()` | Görüş matrisi |
| `Matrix4f getProjectionMatrix()` | Perspektif matrisi |
| `Matrix4f getProjectionViewMatrix()` | Birleşik matris |

### `CameraMode` (Enum)

| Mod | Açıklama | Kontrol |
|-----|----------|---------|
| `FREE` | Serbest uçuş | WASD + QE + Fare |
| `FIRST_PERSON` | Birinci şahıs (FPS) | Fare bakış, oyuncu pozisyonu |
| `RPG_THIRD_PERSON` | RPG 3. şahıs | Oyuncu etrafında orbit |
| `ISOMETRIC` | İzometrik (Strateji) | Sabit 35°/45° açı |
| `EDITOR` | Editör | Shift + WASD + Fare |

```java
Camera camera = new Camera();
camera.setMode(Camera.CameraMode.FIRST_PERSON);
camera.setTarget(player);
```

---

## 9. 🔊 Audio (Ses) — `utils.AudioManager`

OpenAL tabanlı 3D ses sistemi. Tamamen static metodlarla çalışır.

| Metot | Açıklama |
|-------|----------|
| `static void init()` | Ses sistemini başlatır |
| `static void loadSound(String key, String path, boolean loop)` | WAV dosyası yükler |
| `static void playSFX(String key)` | Ses efekti oynatır |
| `static void stopSFX(String key)` | Ses efektini durdurur |
| `static void startBGM()` | Arka plan müziğini başlatır |
| `static void stopBGM()` | Arka plan müziğini durdurur |
| `static void setGain(String key, float gain)` | Ses seviyesi (0-1) |
| `static void setPitch(String key, float pitch)` | Ton yüksekliği |
| `static void updateListenerPosition(float x, y, z)` | Dinleyici (kamera) konumu |
| `static void setSourcePosition(String key, float x, y, z)` | Ses kaynağı konumu |
| `static void setSource3DAttributes(String, float ref, float max, float rolloff)` | 3D sönümleme |
| `static void cleanup()` | Sistemi kapatır, belleği temizler |

```java
AudioManager.init();
AudioManager.loadSound("ocean", "Resources/audio/ocean.wav", true);
AudioManager.setGain("ocean", 0.3f);
AudioManager.setSource3DAttributes("ocean", 100f, 600f, 1.2f);
AudioManager.playSFX("ocean");

// Her frame:
AudioManager.updateListenerPosition(camera.getPosition().x, .y, .z);

// Kapatırken:
AudioManager.cleanup();
```

---

## 10. 🌑 Shadows (Gölge) — `shadows`

### `ShadowMapMasterRenderer`
Cascaded Shadow Mapping sistemi.

| Erişim | Açıklama |
|--------|----------|
| `ShadowBox getShadowBox()` | Gölge kutusu ayarlarına erişim |

### `ShadowBox`

| Metot | Açıklama |
|-------|----------|
| `float getShadowDistance()` | Gölge render mesafesi |
| `void setShadowDistance(float)` | Mesafeyi ayarlar (50-1000) |

```java
ShadowMapMasterRenderer smr = renderEngine.getMasterRenderer().getShadowMapRenderer();
smr.getShadowBox().setShadowDistance(200f);  // 200 birim mesafeye kadar gölge
```

---

## 11. ✨ Particles (Partikül) — `particles`

### `ParticleManager` (Singleton)

| Metot | Açıklama |
|-------|----------|
| `static ParticleManager getInstance()` | Singleton erişim |
| `void update(Scene, float delta)` | Partikülleri günceller |
| `void render(ICamera)` | Partikülleri çizer |
| `void spawnFoam(float x, float y, float z, float size)` | Su köpüğü partikülleri |
| `void spawnParticle(...)` | Genel partikül |

---

## 12. 🖌️ Post Processing — `postProcessing`

Motorda `PostProcessing.currentRenderMode` üzerinden veya oyundayken **[X] tuşu** ile geçiş yapılabilen 4 farklı görsel filtre (Render Mode) mevcuttur:
* **NORMAL:** Standart 3D grafikler.
* **PIXELATE:** Düşük çözünürlük hissi veren mozaik/retro piksel filtresi.
* **GRAYSCALE:** Tamamen siyah-beyaz (dramatik/Luma) renk dönüşümü.
* **CARTOON:** Sobel kenar tespiti (Luma bazlı) ve renk posterizasyonu (gamma düzeltmeli) ile elde edilen Low-Poly/Çizgi Film stili.

| Sınıf | Açıklama |
|-------|----------|
| `PostProcessing` | Pipeline yöneticisi (RenderMode yönetimini içerir) |
| `Fbo` | Frame Buffer Object soyutlaması |
| `ContrastFilter` | Kontrast ayarı |
| `HorizontalBlur` / `VerticalBlur` | Gaussian Blur |
| `CombineFilter` | Bloom birleştirme |
| `PixelateFilter` | Pixel Art efekti |
| `GrayscaleFilter` | Siyah Beyaz (Monochrome) efekti |
| `CartoonFilter` | Karton (Cel-Shading) efekti |

---

## 13. 🖥️ GUI — `guiRendering`

### `UIManager`
2D arayüz yöneticisi (butonlar, mesajlar).

| Metot | Açıklama |
|-------|----------|
| `void addButton(x, y, w, h, text, Runnable)` | Tıklanabilir buton ekler |
| `void showMessage(String)` | Ekrana geçici mesaj gösterir |
| `void render()` | UI elemanlarını çizer |

### `UIButton`
Tıklanabilir buton bileşeni.

### `UITheme`
Tema ve renk paleti sistemi.

### `OpenglYaziCizimi`
OpenGL tabanlı metin çizim motoru.

---

## 14. 📦 Loaders (Yükleyiciler) — `loaders`

| Sınıf | Açıklama |
|-------|----------|
| `EntityLoader` | Entity nesnelerini dosyadan yükler |
| `SceneLoader` | Tüm sahneyi dosyadan yükler |
| `ModelLoader` | 3D model yükler |
| `SkinLoader` | Materyal/doku seti yükler |
| `SkyboxLoader` | Gökyüzü cubemap yükler |
| `Configs` / `ConfigsLoader` | Motor yapılandırma dosyaları |

---

## 15. 🖼️ Textures (Dokular) — `textures`

| Sınıf | Açıklama |
|-------|----------|
| `Texture` | GPU'daki doku referansı |
| `TextureBuilder` | Doku yapılandırma (filtre, mipmap, wrap) |
| `TextureUtils` | Doku yükleme yardımcıları |
| `TextureData` | Ham doku verisi |

---

## 16. 🔮 OpenGL Nesneleri — `openglObjects`

| Sınıf | Açıklama |
|-------|----------|
| `Vao` | Vertex Array Object soyutlaması |
| `Vbo` | Vertex Buffer Object soyutlaması |
| `Query` | GPU sorguları (Occlusion query vb.) |

---

## 17. ☀️ Sun & Lens Flare — `sunRenderer`, `lensFlare`

### `Sun`
Güneş nesnesi (pozisyon, boyut).

### `SunRenderer`
Güneş billboard çizimi.

### `FlareManager`
Güneşe bakıldığında oluşan lens parlaması efektlerini yönetir.

---

## 18. 🌐 Network — `network`

| Sınıf | Açıklama |
|-------|----------|
| `GameServer` | TCP tabanlı oyun sunucusu |
| `GameClient` | TCP tabanlı oyun istemcisi |
| `Packet` | Ağ paket yapısı |
| `SteamP2PManager` | Steam P2P bağlantı yöneticisi |

---

## 19. 🎮 Steam — `steam`

### `SteamManager`
Steamworks SDK entegrasyonu (başarımlar, arkadaşlar, multiplayer).

---

## 20. 🧱 Hazır Nesneler — `objects`

| Sınıf | Açıklama |
|-------|----------|
| `Tree3D` | Hazır 3D ağaç modeli (LOD destekli) |
| `Grass3D` | Hazır 3D çimen modeli |
| `WLL` | Marker / işaret ışığı nesnesi |
| `hus` | Ev / yapı nesnesi |

```java
// Ağaç ekleme
Entity tree = new objects.Tree3D();
tree.getPosition().set(50, 0, 100);
tree.setScale(1.5f);
scene.addEntity(tree);

// Işık marker ekleme
Entity marker = new objects.WLL();
marker.getPosition().set(10, 5, 10);
marker.setScale(0.02f);
scene.addEntity(marker);
scene.addPointLight(new Light(
    new Vector3f(10, 5, 10),
    new Vector3f(100, 80, 20),
    new Vector3f(1, 0.005f, 0.0005f)
));
```

---

## 21. 🎮 Default Controls (Varsayılan Kontrolcüler) — `default_controls`

Oyun motoruna eklenen hazır nesneler için önceden kodlanmış fizik/oyun kontrol komponentleri.

| Sınıf | Açıklama |
|-------|----------|
| `BirdPlayerController` | Oyuncunun uçan bir kuşu kontrol etmesini sağlar (Hız, kanat çırpma, süzülme, yerçekimi). |
| `BirdAIController` | NPC kuşların yapay zeka ile etrafta rastgele uçmalarını sağlar. |
| `ShipController` | Gemi fiziğini simüle eder. Su sürtünmesi (Drag), ivmelenme, dümen (Rudder), gaz kolu (Throttle) ve dalga beşik hareketlerini (Bobbing/Roll/Pitch) içerir. |
| `FishPlayerController` | Oyuncunun bir balığı WASD ile yüzdürmesini ve yönlendirmesini sağlar (Suyun altı fiziği). |
| `FishController` | Denizdeki NPC balıkların rastgele derinliklerde ve yönlerde yüzmesini (AI) sağlar. |

---

## 22. 🛠️ Utilities — `utils`

| Sınıf | Açıklama |
|-------|----------|
| `DisplayManager` | Pencere/ekran oluşturma ve yönetimi |
| `MousePicker` | Fare ışını (Ray Casting) ile 3D seçim |
| `Frustum` | Görüş kesiti (Frustum Culling) hesaplama |
| `SmoothFloat` | Değerlerde yumuşak geçiş (Lerp/Damping) |
| `CinematicCamera` | Keyframe tabanlı sinematik kamera |
| `CameraKeyframe` | Sinematik anahtar kare verisi |
| `ICamera` | Kamera arayüzü |
| `OpenGlUtils` | OpenGL durum ayarları (depth, blend) |
| `MyFile` | Dosya okuma yardımcısı |
| `NativeLibraryLoader` | Native DLL/SO yükleyici |
| `Text3DBuilder` | 3D metin oluşturucu |

---

## 23. 🎨 Shader Altyapısı — `shaders`

| Sınıf | Açıklama |
|-------|----------|
| `ShaderProgram` | GLSL vertex/fragment shader derleyici |
| `ComputeShader` | Compute shader desteği |
| `Uniform` | Shader uniform değişken temeli |
| `UniformFloat` | `float` uniform |
| `UniformInt` | `int` uniform |
| `UniformVec2/3/4` | Vektör uniform'ları |
| `UniformMatrix` | Matris uniform |
| `UniformBoolean` | Boolean uniform |
| `UniformSampler` | Texture sampler uniform |

---

## 24. 🖥️ Gane IDE (Görsel Arayüz) — `ide`

Gane Engine, Unity veya Unreal Engine benzeri tam teşekküllü bir görsel düzenleyici ile birlikte gelir! `Engine/ide/GaneIDE.java` sınıfı üzerinden başlatılabilir. 

| Sınıf / Panel | Açıklama |
|-------|----------|
| `GaneIDE` | Arayüzün ana başlatıcısıdır (Java Swing). |
| `ViewportCanvas` | Oyunun render edildiği 3D ekrandır. Fare sol tıkı ile objeleri seçmenizi (Mouse Picking) sağlayan raycast algoritmasını barındırır. |
| `HierarchyPanel` | Sahnedeki tüm objeleri listeler. Bilgisayardan sürükle-bırak mantığıyla `.glb` modeli eklemenize olanak tanır. |
| `InspectorPanel` | Seçili objenin X,Y,Z konumunu ve rotasyonunu düzenler. **Add/Edit Script** butonu sayesinde objelere anlık olarak Java kodu yazıp bağlamanızı sağlar. |
| `EnvironmentPanel` | Gece/Gündüz döngüsünü kaydırıcı ile yönetmenizi, okyanus rengini ve dalga boyunu anlık değiştirmenizi sağlar. |
| `SceneSerializer` | Sahnenizi `*.gane` uzantılı bir JSON dosyası olarak kaydeder ve yükler (Scriptler ve Environment ayarları dahil). |
| `RuntimeCompiler` | Java 8+ / 17+ kodlarını oyun motoru çalışırken (çalışma zamanında) derleyerek (Runtime Compilation) anında objelere Component olarak yükler. Oyunu veya IDE'yi kapatmanıza gerek kalmaz! |

---

## 📋 Hızlı Başlangıç — Minimal Oyun Döngüsü

```java
package gane;

import renderEngine.RenderEngine;
import scene.*;
import extra.Camera;
import terrain.FlatTerrain;
import water.WaterTile;
import skybox.AtmosphereSky;
import environment.DayNightManager;
import physics.PhysicsEngine;

public class MyGame {
    public static void main(String[] args) {
        // 1. Motor başlat
        RenderEngine engine = RenderEngine.init();
        utils.AudioManager.init();

        // 2. Kamera
        Camera camera = new Camera();
        camera.setMode(Camera.CameraMode.FREE);
        camera.getPosition().set(0, 50, 0);

        // 3. Gökyüzü
        AtmosphereSky sky = new AtmosphereSky();

        // 4. Sahne
        Scene scene = new Scene(camera, sky, true);

        // 5. Arazi
        FlatTerrain terrain = new FlatTerrain(512, 512);
        scene.addTerrain(terrain);

        // 6. Su
        WaterTile ocean = new WaterTile(0, 0, -2f, 5000f);
        ocean.setBaseColor(0.01f, 0.22f, 0.36f);
        scene.addWater(ocean);

        // 7. Gece/Gündüz
        DayNightManager dayNight = new DayNightManager(scene, 10f, 600f);

        // 8. Fizik
        PhysicsEngine physics = new PhysicsEngine();

        // 9. Oyun döngüsü
        while (!org.lwjgl.opengl.Display.isCloseRequested()) {
            float delta = engine.getDisplayManager().getFrameTime();

            camera.move();
            dayNight.update(delta);
            physics.update(scene, delta);
            engine.renderScene(scene, delta);
            engine.update();
        }

        // 10. Temizlik
        scene.delete();
        utils.AudioManager.cleanup();
        engine.close();
    }
}
```

---

> 📖 **Bu kılavuz**, Gane Engine'in tüm alt sistemlerini, sınıflarını ve fonksiyonlarını
> geliştiricilerin hızlıca referans alabilmesi için düzenlenmiştir.
