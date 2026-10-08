package com.exoticworlds.compat.reterraforged;

public final class ReTerraForgedInjectionTargets {
    // ReTerraForged is compiled per loader: its override of DensityFunction.compute carries the Mojmap name in the
    // NeoForge jar and the intermediary one in the Fabric jar, and the remapper cannot resolve an override it only
    // meets in a foreign class, so both names are listed and the running loader's one matches.
    public static final String COMPUTE = "compute";

    public static final String COMPUTE_INTERMEDIARY = "method_40464";

    public static final String CELL_CONTINENT_X =
            "Lraccoonman/reterraforged/world/worldgen/cell/Cell;continentX:I";

    public static final String CELL_CONTINENT_Z =
            "Lraccoonman/reterraforged/world/worldgen/cell/Cell;continentZ:I";

    public static final String NOISE_COMPUTE =
            "Lraccoonman/reterraforged/world/worldgen/noise/module/Noise;compute(FFI)F";

    private ReTerraForgedInjectionTargets() {
    }
}
