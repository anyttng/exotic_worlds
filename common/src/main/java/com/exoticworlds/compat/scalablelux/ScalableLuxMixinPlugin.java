package com.exoticworlds.compat.scalablelux;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;
import com.exoticworlds.compat.c2me.C2meLightingLock;

public class ScalableLuxMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String SCHEDULING_LOCK_MIXIN = "SchedulingUtilLockMixin";
    private static final String SCHEDULING_TOKENS_MIXIN = "SchedulingUtilTokensMixin";

    private static final ModPresence SCALABLELUX = ModPresence.gate(LOGGER,
                    "[scalablelux-compat] gate scalablelux_present")
            .probing("ca/spottedleaf/starlight/common/light/StarLightInterface.class")
            .checking("exotic_worlds.compat.scalablelux.mixins.json")
            .withholding(SCHEDULING_LOCK_MIXIN, ScalableLuxMixinPlugin::c2meOwnsTheLock)
            .bodyFrom(SCHEDULING_TOKENS_MIXIN, C2meLightingLock.OVERWRITE_CLASS)
            .build();

    public ScalableLuxMixinPlugin() {
        super(SCALABLELUX);
    }

    private static boolean c2meOwnsTheLock() {
        return ModPresence.probe(C2meLightingLock.OVERWRITE_RESOURCE);
    }
}
