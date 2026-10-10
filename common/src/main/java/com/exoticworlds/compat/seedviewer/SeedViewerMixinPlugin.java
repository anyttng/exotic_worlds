package com.exoticworlds.compat.seedviewer;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class SeedViewerMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence SEED_VIEWER = ModPresence.gate(LOGGER, "[seedviewer-compat] gate seedviewer_present")
            .probing("net/acenia/seedviewer/client/worldgen/BiomeSampler.class")
            .checking("exotic_worlds.compat.seedviewer.mixins.json")
            .build();

    public SeedViewerMixinPlugin() {
        super(SEED_VIEWER);
    }
}
