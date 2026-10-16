package com.exoticworlds.compat.reterraforged;

public final class ReTerraForgedInjectionTargets {
    // Both names: each loader's ReTerraForged jar carries its own, and the remapper cannot resolve a foreign override.
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
