package com.exoticworlds.compat.mekanism;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.gate.ModPresence;
import com.exoticworlds.compat.gate.ModPresenceGatePlugin;

public class MekanismMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MEKANISM_PACKAGE = "mekanism/";

    private static final ModPresence MEKANISM = ModPresence.gate(LOGGER, "[ew/mek-compat] gate mekanism_present")
            .probing("mekanism/common/Mekanism.class")
            .checking("exotic_worlds.compat.mekanism.mixins.json")
            .modPackages(MEKANISM_PACKAGE)
            .build();

    public MekanismMixinPlugin() {
        super(MEKANISM);
    }
}
