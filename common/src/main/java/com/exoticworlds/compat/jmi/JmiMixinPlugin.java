package com.exoticworlds.compat.jmi;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class JmiMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence JMI = ModPresence.gate(LOGGER, "[jmi-compat] gate jmi_present")
            .probing("me/frankv/jmi/JMI.class")
            .checking("exotic_worlds.compat.jmi.mixins.json")
            .build();

    public JmiMixinPlugin() {
        super(JMI);
    }
}
