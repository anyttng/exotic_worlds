package com.exoticworlds.compat.lithostitched;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class LithostitchedMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LITHOSTITCHED_PACKAGE = "dev/worldgen/lithostitched/";

    private static final ModPresence LITHOSTITCHED = ModPresence
            .gate(LOGGER, "[ew/lithostitched-compat] gate lithostitched_present")
            .probing("dev/worldgen/lithostitched/worldgen/placementmodifier/NoiseSlopePlacement.class")
            .checking("exotic_worlds.compat.lithostitched.mixins.json")
            .modPackages(LITHOSTITCHED_PACKAGE)
            .build();

    public LithostitchedMixinPlugin() {
        super(LITHOSTITCHED);
    }
}
