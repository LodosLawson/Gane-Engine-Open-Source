package terrain;

/**
 * @deprecated Bu sınıf terrain.planet.PlanetTerrain'e taşındı.
 * Lütfen importları güncelleyin:
 *   import terrain.planet.PlanetTerrain;
 */
@Deprecated
public class PlanetTerrain extends terrain.planet.PlanetTerrain {
    public PlanetTerrain(float radius, int subdivisions, terrain.flat.FlatTerrainShader shader) {
        super(radius, subdivisions, shader);
    }
}
