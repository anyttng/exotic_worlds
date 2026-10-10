package com.exoticworlds.compat.simpleclouds;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class SimpleCloudsMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence SIMPLE_CLOUDS = ModPresence.gate(LOGGER, "[sc-compat] gate simpleclouds_present")
            .probing("dev/nonamecrackers2/simpleclouds/SimpleCloudsMod.class")
            .checking("exotic_worlds.compat.simpleclouds.mixins.json")
            .build();

    public SimpleCloudsMixinPlugin() {
        super(SIMPLE_CLOUDS);
    }
}
