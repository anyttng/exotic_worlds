package com.exoticworlds.compat.simpleclouds;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;
import com.exoticworlds.compat.ModSymbol;

public class SimpleCloudsMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    static final ModSymbol CLOUD_MANAGER_PRECIPITATION = new ModSymbol(
            "dev/nonamecrackers2/simpleclouds/common/world/CloudManager", "getPrecipitationAt",
            "(Lnet/minecraft/core/BlockPos;)Lorg/apache/commons/lang3/tuple/Pair;");

    private static final ModPresence SIMPLE_CLOUDS = ModPresence.of(LOGGER,
            "dev/nonamecrackers2/simpleclouds/SimpleCloudsMod.class", "[sc-compat] gate simpleclouds_present",
            CLOUD_MANAGER_PRECIPITATION);

    public SimpleCloudsMixinPlugin() {
        super(SIMPLE_CLOUDS);
    }
}
