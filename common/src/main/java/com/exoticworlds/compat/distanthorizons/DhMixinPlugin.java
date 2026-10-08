package com.exoticworlds.compat.distanthorizons;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class DhMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence DH = ModPresence.gate(LOGGER, "[dh-compat] gate distanthorizons_present")
            .probing("com/seibel/distanthorizons/core/api/internal/ClientApi.class")
            .checking("exotic_worlds.compat.distanthorizons.mixins.json")
            .build();

    public DhMixinPlugin() {
        super(DH);
    }
}
