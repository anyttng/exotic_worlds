package com.exoticworlds.compat.ultraterraforged;

public final class UltraTerraForgedInjectionTargets {
    public static final String CELL_CONTINENT_X =
            "Lcom/pandaismyname1/ultraterraforged/world/worldgen/cell/Cell;continentX:I";

    public static final String CELL_CONTINENT_Z =
            "Lcom/pandaismyname1/ultraterraforged/world/worldgen/cell/Cell;continentZ:I";

    public static final String LANDFORM_RANDOM =
            "Lcom/pandaismyname1/ultraterraforged/world/worldgen/landform/Landform;random(IIII)F";

    public static final String SITE_CACHE_GET = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/landform/SiteCache;"
            + "get(IILcom/pandaismyname1/ultraterraforged/world/worldgen/landform/SiteCache$Finder;)Ljava/lang/Object;";

    public static final String CAVE_RANDOM =
            "Lcom/pandaismyname1/ultraterraforged/world/worldgen/cave/CaveFeatures;random(IIII)F";

    public static final String NOISE_COMPUTE =
            "Lcom/pandaismyname1/ultraterraforged/world/worldgen/noise/module/Noise;compute(FFI)F";

    private UltraTerraForgedInjectionTargets() {
    }
}
