package com.exoticworlds.compat.journeymap;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;
import com.exoticworlds.compat.ModSymbol;

public class JourneyMapMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    static final ModSymbol MAP_RENDERER_CENTRE =
            new ModSymbol("journeymap/client/render/map/MapRenderer", "centerBlockX", "D");

    private static final ModPresence JOURNEYMAP = ModPresence.of(LOGGER,
            "journeymap/client/JourneymapClient.class", "[jm-compat] gate jm_present", MAP_RENDERER_CENTRE);

    public JourneyMapMixinPlugin() {
        super(JOURNEYMAP);
    }
}
