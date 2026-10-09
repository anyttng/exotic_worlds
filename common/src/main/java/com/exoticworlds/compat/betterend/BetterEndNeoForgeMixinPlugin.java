package com.exoticworlds.compat.betterend;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class BetterEndNeoForgeMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence BETTER_END = ModPresence.gate(LOGGER, "[betterend-compat] gate betterend_neoforge_present")
            .probing("org/betterx/betterend/world/generator/TerrainGenerator.class")
            .checking("exotic_worlds.compat.betterend.neoforge.mixins.json")
            .build();

    public BetterEndNeoForgeMixinPlugin() {
        super(BETTER_END);
    }
}
