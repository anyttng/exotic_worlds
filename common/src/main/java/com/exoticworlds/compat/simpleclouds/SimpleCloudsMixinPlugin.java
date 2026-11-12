package com.exoticworlds.compat.simpleclouds;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.gate.ModPresence;
import com.exoticworlds.compat.gate.ModPresenceGatePlugin;

public class SimpleCloudsMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SIMPLE_CLOUDS_PACKAGE = "dev/nonamecrackers2/simpleclouds/";

    public static final ModPresence SIMPLE_CLOUDS =ModPresence.gate(LOGGER, "[ew/sc-compat] gate simpleclouds_present")
            .probing("dev/nonamecrackers2/simpleclouds/SimpleCloudsMod.class")
            .checking("exotic_worlds.compat.simpleclouds.mixins.json")
            .modPackages(SIMPLE_CLOUDS_PACKAGE)
            .build();

    public SimpleCloudsMixinPlugin() {
        super(SIMPLE_CLOUDS);
    }
}
