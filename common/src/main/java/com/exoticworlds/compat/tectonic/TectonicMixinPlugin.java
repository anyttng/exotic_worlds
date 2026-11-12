package com.exoticworlds.compat.tectonic;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.gate.ModPresence;
import com.exoticworlds.compat.gate.ModPresenceGatePlugin;

public class TectonicMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TECTONIC_PACKAGE = "dev/worldgen/tectonic/";

    private static final ModPresence TECTONIC = ModPresence
            .gate(LOGGER, "[ew/tectonic-compat] gate tectonic_present")
            .probing("dev/worldgen/tectonic/worldgen/densityfunction/ConfigNoise.class")
            .checking("exotic_worlds.compat.tectonic.mixins.json")
            .modPackages(TECTONIC_PACKAGE)
            .build();

    public TectonicMixinPlugin() {
        super(TECTONIC);
    }
}
