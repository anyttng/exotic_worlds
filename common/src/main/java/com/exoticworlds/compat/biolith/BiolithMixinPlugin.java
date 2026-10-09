package com.exoticworlds.compat.biolith;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class BiolithMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence BIOLITH = ModPresence.gate(LOGGER, "[biolith-compat] gate biolith_present")
            .probing("com/terraformersmc/biolith/impl/biome/OverworldBiomePlacement.class")
            .checking("exotic_worlds.compat.biolith.mixins.json")
            .build();

    public BiolithMixinPlugin() {
        super(BIOLITH);
    }
}
