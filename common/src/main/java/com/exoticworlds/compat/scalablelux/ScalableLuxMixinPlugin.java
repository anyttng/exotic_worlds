package com.exoticworlds.compat.scalablelux;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;
import com.exoticworlds.compat.ModSymbol;
import com.exoticworlds.compat.c2me.C2meLightingLock;

public class ScalableLuxMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String SCHEDULING_LOCK_MIXIN = "SchedulingUtilLockMixin";

    static final ModSymbol ANY_CHUNK_NOW = new ModSymbol(
            "ca/spottedleaf/starlight/common/light/StarLightInterface", "getAnyChunkNow",
            "(II)Lnet/minecraft/world/level/chunk/ChunkAccess;");

    static final ModSymbol VANILLA_INTERFACE_CLOSE = new ModSymbol(
            "ca/spottedleaf/starlight/common/light/vanillainterface/ThreadedLevelLightEngineVanillaInterface",
            "close", "()V");

    private static final ModPresence SCALABLELUX = ModPresence.of(LOGGER,
            "ca/spottedleaf/starlight/common/light/StarLightInterface.class",
            "[scalablelux-compat] gate scalablelux_present", ANY_CHUNK_NOW, VANILLA_INTERFACE_CLOSE);

    public ScalableLuxMixinPlugin() {
        super(SCALABLELUX);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith(SCHEDULING_LOCK_MIXIN)) {
            return SCALABLELUX.present() && !ModPresence.probe(C2meLightingLock.OVERWRITE_RESOURCE);
        }

        return super.shouldApplyMixin(targetClassName, mixinClassName);
    }
}
