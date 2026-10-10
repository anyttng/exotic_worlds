package com.exoticworlds.compat.wover;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class WoverMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence WOVER = ModPresence.gate(LOGGER, "[wover-compat] gate wover_present")
            .probing("org/betterx/wover/generator/impl/map/hex/HexBiomeMap.class")
            .checking("exotic_worlds.compat.wover.mixins.json")
            .build();

    public WoverMixinPlugin() {
        super(WOVER);
    }
}
