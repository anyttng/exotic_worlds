package com.exoticworlds.compat.terrablender;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class TerraBlenderMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence TERRABLENDER = ModPresence.gate(LOGGER,
                    "[terrablender-compat] gate terrablender_present")
            .probing("terrablender/worldgen/noise/Area.class")
            .checking("exotic_worlds.compat.terrablender.mixins.json")
            .build();

    public TerraBlenderMixinPlugin() {
        super(TERRABLENDER);
    }
}
