package com.exoticworlds.compat.mapfrontiers;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class MapFrontiersMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence MAPFRONTIERS = ModPresence
            .gate(LOGGER, "[ew/mapfrontiers-compat] gate mapfrontiers_present")
            .probing("games/alejandrocoria/mapfrontiers/platform/services/WorldGeometry.class")
            .checking("exotic_worlds.compat.mapfrontiers.mixins.json")
            .build();

    public MapFrontiersMixinPlugin() {
        super(MAPFRONTIERS);
    }
}
