package com.exoticworlds.compat.mapatlases;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.gate.ModPresence;
import com.exoticworlds.compat.gate.ModPresenceGatePlugin;

public class MapAtlasesMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MAP_ATLASES_PACKAGE = "pepjebs/mapatlases/";

    static final ModPresence MAP_ATLASES = ModPresence
            .gate(LOGGER, "[ew/mapatlases-compat] gate mapatlases_present")
            .probing("pepjebs/mapatlases/map_collection/MapCollection.class")
            .checking("exotic_worlds.compat.mapatlases.mixins.json")
            .modPackages(MAP_ATLASES_PACKAGE)
            .build();

    public MapAtlasesMixinPlugin() {
        super(MAP_ATLASES);
    }
}
