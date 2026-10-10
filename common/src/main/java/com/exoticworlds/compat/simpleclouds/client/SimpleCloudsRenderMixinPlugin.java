package com.exoticworlds.compat.simpleclouds.client;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class SimpleCloudsRenderMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence SIMPLE_CLOUDS_RENDER = ModPresence.gate(LOGGER, "[sc-compat] gate simpleclouds_render_present")
            .probing("dev/nonamecrackers2/simpleclouds/SimpleCloudsMod.class")
            .checking("exotic_worlds.compat.simpleclouds.client.mixins.json")
            .build();

    public SimpleCloudsRenderMixinPlugin() {
        super(SIMPLE_CLOUDS_RENDER);
    }
}
