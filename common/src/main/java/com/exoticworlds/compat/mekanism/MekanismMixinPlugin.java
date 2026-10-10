package com.exoticworlds.compat.mekanism;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class MekanismMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence MEKANISM = ModPresence.gate(LOGGER, "[mek-compat] gate mekanism_present")
            .probing("mekanism/common/Mekanism.class")
            .checking("exotic_worlds.compat.mekanism.mixins.json")
            .build();

    public MekanismMixinPlugin() {
        super(MEKANISM);
    }
}
