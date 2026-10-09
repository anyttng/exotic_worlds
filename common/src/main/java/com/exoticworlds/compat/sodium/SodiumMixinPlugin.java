package com.exoticworlds.compat.sodium;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class SodiumMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence SODIUM = ModPresence.gate(LOGGER, "[sodium-compat] gate sodium_present")
            .probing("net/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer.class")
            .checking("exotic_worlds.compat.sodium.mixins.json")
            .build();

    public SodiumMixinPlugin() {
        super(SODIUM);
    }
}
