package com.exoticworlds.compat.simpleclouds.client;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;
import com.exoticworlds.compat.ModSymbol;

public class SimpleCloudsRenderMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModSymbol REGION_UPLOAD = new ModSymbol(
            "dev/nonamecrackers2/simpleclouds/client/mesh/generator/MultiRegionCloudMeshGenerator",
            "lambda$uploadCloudRegionData$4", "(FLdev/nonamecrackers2/simpleclouds/common/cloud/region/CloudRegion;)[F");

    private static final ModPresence SIMPLE_CLOUDS_RENDER = ModPresence.of(LOGGER,
            "dev/nonamecrackers2/simpleclouds/SimpleCloudsMod.class", "[sc-compat] gate simpleclouds_render_present",
            REGION_UPLOAD);

    public SimpleCloudsRenderMixinPlugin() {
        super(SIMPLE_CLOUDS_RENDER);
    }
}
