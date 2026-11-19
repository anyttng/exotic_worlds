package com.exoticworlds.compat.vista;

import org.slf4j.Logger;

import com.exoticworlds.compat.gate.ModPresence;
import com.exoticworlds.compat.gate.ModPresenceGatePlugin;
import com.mojang.logging.LogUtils;

public class VistaMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String VISTA_PACKAGE = "net/mehvahdjukaar/vista/";
    private static final String MOONLIGHT_PACKAGE = "net/mehvahdjukaar/moonlight/";

    static final ModPresence VISTA = ModPresence.gate(LOGGER, "[ew/vista-compat] gate vista_present")
            .probing("net/mehvahdjukaar/vista/network/ClientBoundSyncExtraChunksPacket.class")
            .checking("exotic_worlds.compat.vista.mixins.json")
            .modPackages(VISTA_PACKAGE, MOONLIGHT_PACKAGE)
            .build();

    public VistaMixinPlugin() {
        super(VISTA);
    }
}
