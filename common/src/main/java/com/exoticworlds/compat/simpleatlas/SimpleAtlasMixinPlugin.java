package com.exoticworlds.compat.simpleatlas;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class SimpleAtlasMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SIMPLE_ATLAS_PACKAGE = "rubbertoe/simple_atlas/";

    private static final ModPresence SIMPLE_ATLAS = ModPresence.gate(LOGGER, "[ew/simpleatlas-compat] gate simpleatlas_present")
            .probing("rubbertoe/simple_atlas/map/AtlasMapSelector.class")
            .checking("exotic_worlds.compat.simpleatlas.mixins.json")
            .modPackages(SIMPLE_ATLAS_PACKAGE)
            .build();

    public SimpleAtlasMixinPlugin() {
        super(SIMPLE_ATLAS);
    }
}
