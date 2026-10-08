package com.exoticworlds.compat.journeymap;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class JourneyMapMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence JOURNEYMAP = ModPresence.gate(LOGGER, "[jm-compat] gate jm_present")
            .probing("journeymap/client/JourneymapClient.class")
            .checking("exotic_worlds.compat.journeymap.mixins.json")
            .build();

    public JourneyMapMixinPlugin() {
        super(JOURNEYMAP);
    }
}
