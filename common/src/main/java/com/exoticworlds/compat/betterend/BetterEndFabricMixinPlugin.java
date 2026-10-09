package com.exoticworlds.compat.betterend;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class BetterEndFabricMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence BETTER_END = ModPresence.gate(LOGGER, "[betterend-compat] gate betterend_fabric_present")
            .probing("org/betterx/betterend/world/generator/TerrainGenerator.class")
            .checking("exotic_worlds.compat.betterend.fabric.mixins.json")
            .build();

    public BetterEndFabricMixinPlugin() {
        super(BETTER_END);
    }
}
