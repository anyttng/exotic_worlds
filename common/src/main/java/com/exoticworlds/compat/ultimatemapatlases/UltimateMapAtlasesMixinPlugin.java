package com.exoticworlds.compat.ultimatemapatlases;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class UltimateMapAtlasesMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NANAKY_PACKAGE = "net/nanaky/";

    static final ModPresence ULTIMATE_MAP_ATLASES = ModPresence
            .gate(LOGGER, "[ew/ultimatemapatlases-compat] gate ultimatemapatlases_present")
            .probing("net/nanaky/ultimate_map_atlases/map_collection/MapCollection.class")
            .checking("exotic_worlds.compat.ultimatemapatlases.mixins.json")
            .modPackages(NANAKY_PACKAGE)
            .build();

    public UltimateMapAtlasesMixinPlugin() {
        super(ULTIMATE_MAP_ATLASES);
    }
}
