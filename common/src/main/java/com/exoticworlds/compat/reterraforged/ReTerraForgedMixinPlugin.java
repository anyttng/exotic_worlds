package com.exoticworlds.compat.reterraforged;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;
import com.exoticworlds.compat.ModSymbol;

public class ReTerraForgedMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    static final ModSymbol LATTICE_HASH = new ModSymbol(
            "raccoonman/reterraforged/world/worldgen/noise/NoiseUtil", "hash2D", "(III)I");

    static final ModSymbol HEIGHTMAP_RIVERS = new ModSymbol(
            "raccoonman/reterraforged/world/worldgen/cell/heightmap/Heightmap", "applyRivers",
            "(Lraccoonman/reterraforged/world/worldgen/cell/Cell;FF"
                    + "Lraccoonman/reterraforged/world/worldgen/cell/rivermap/Rivermap;)V");

    static final ModSymbol UPLIFT_GRADIENT = new ModSymbol(
            "raccoonman/reterraforged/world/worldgen/cell/continent/uplift/UpliftContinentGenerator",
            "getSmoothVoronoiGradient", "(Lraccoonman/reterraforged/world/worldgen/cell/Cell;FF)F");

    static final ModSymbol POISSON_POINT = new ModSymbol(
            "raccoonman/reterraforged/world/worldgen/feature/placement/poisson/FastPoisson", "getPoint",
            "(IFFLraccoonman/reterraforged/world/worldgen/feature/placement/poisson/FastPoissonContext;)J");

    private static final ModPresence RETERRAFORGED = ModPresence.of(LOGGER,
            "raccoonman/reterraforged/world/worldgen/cell/heightmap/Heightmap.class",
            "[reterraforged-compat] gate reterraforged_present", LATTICE_HASH, HEIGHTMAP_RIVERS, UPLIFT_GRADIENT,
            POISSON_POINT);

    public ReTerraForgedMixinPlugin() {
        super(RETERRAFORGED);
    }
}
