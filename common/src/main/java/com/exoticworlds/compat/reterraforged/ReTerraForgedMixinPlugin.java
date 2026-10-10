package com.exoticworlds.compat.reterraforged;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class ReTerraForgedMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence RETERRAFORGED = ModPresence.gate(LOGGER, "[reterraforged-compat] gate reterraforged_present")
            .probing("raccoonman/reterraforged/world/worldgen/cell/heightmap/Heightmap.class")
            .checking("exotic_worlds.compat.reterraforged.mixins.json")
            .build();

    public ReTerraForgedMixinPlugin() {
        super(RETERRAFORGED);
    }
}
