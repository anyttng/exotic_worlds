package com.exoticworlds.compat.eminus;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.gate.ModPresence;
import com.exoticworlds.compat.gate.ModPresenceGatePlugin;

public class EminusMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String EMINUS_PACKAGE = "com/eminus/";

    private static final ModPresence EMINUS = ModPresence.gate(LOGGER, "[ew/eminus-compat] gate eminus_present")
            .probing("com/eminus/cell/cache/CellCache.class")
            .checking("exotic_worlds.compat.eminus.mixins.json")
            .modPackages(EMINUS_PACKAGE)
            .build();

    public EminusMixinPlugin() {
        super(EMINUS);
    }
}
