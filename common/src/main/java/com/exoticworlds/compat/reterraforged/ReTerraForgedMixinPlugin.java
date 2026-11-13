package com.exoticworlds.compat.reterraforged;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.gate.ModPresence;
import com.exoticworlds.compat.gate.ModPresenceGatePlugin;

public class ReTerraForgedMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String RETERRAFORGED_PACKAGE = "raccoonman/reterraforged/";
    private static final String SPAWN_FINDER_MIXIN = RETERRAFORGED_PACKAGE + "mixin/MixinSpawnFinder";

    private static final ModPresence RETERRAFORGED = ModPresence.gate(LOGGER, "[ew/reterraforged-compat] gate reterraforged_present")
            .probing("raccoonman/reterraforged/world/worldgen/cell/heightmap/Heightmap.class")
            .checking("exotic_worlds.compat.reterraforged.mixins.json")
            .bodyFrom("SpawnFinderOriginMixin", SPAWN_FINDER_MIXIN)
            .bodyFrom("SpawnFinderUserSelectedMixin", SPAWN_FINDER_MIXIN)
            .modPackages(RETERRAFORGED_PACKAGE)
            .build();

    public ReTerraForgedMixinPlugin() {
        super(RETERRAFORGED);
    }
}
