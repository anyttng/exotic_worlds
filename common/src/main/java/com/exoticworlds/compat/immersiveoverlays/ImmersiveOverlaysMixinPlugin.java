package com.exoticworlds.compat.immersiveoverlays;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class ImmersiveOverlaysMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence IMMERSIVE_OVERLAYS = ModPresence
            .gate(LOGGER, "[ew/immersiveoverlays-compat] gate immersiveoverlays_present")
            .probing("cc/cassian/immersiveoverlays/overlay/CompassOverlay.class")
            .checking("exotic_worlds.compat.immersiveoverlays.mixins.json")
            .build();

    public ImmersiveOverlaysMixinPlugin() {
        super(IMMERSIVE_OVERLAYS);
    }
}
