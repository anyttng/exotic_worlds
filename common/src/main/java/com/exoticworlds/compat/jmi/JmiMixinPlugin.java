package com.exoticworlds.compat.jmi;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.gate.ModPresence;
import com.exoticworlds.compat.gate.ModPresenceGatePlugin;
import com.exoticworlds.compat.journeymap.JourneyMapMixinPlugin;

public class JmiMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String JMI_PACKAGE = "me/frankv/jmi/";
    private static final String JOURNEYMAP_API_PACKAGE = "journeymap/api/v2/";

    private static final ModPresence JMI = ModPresence.gate(LOGGER, "[ew/jmi-compat] gate jmi_present")
            .probing("me/frankv/jmi/JMI.class")
            .checking("exotic_worlds.compat.jmi.mixins.json")
            .requiring(JourneyMapMixinPlugin.JOURNEYMAP)
            .modPackages(JMI_PACKAGE, JOURNEYMAP_API_PACKAGE)
            .build();

    public JmiMixinPlugin() {
        super(JMI);
    }
}
