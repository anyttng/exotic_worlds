package com.exoticworlds.compat.arsnouveau;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class ArsNouveauMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence ARS_NOUVEAU = ModPresence.gate(LOGGER, "[ew/ars-compat] gate ars_nouveau_present")
            .probing("com/hollingsworth/arsnouveau/ArsNouveau.class")
            .checking("exotic_worlds.compat.arsnouveau.mixins.json")
            .build();

    public ArsNouveauMixinPlugin() {
        super(ARS_NOUVEAU);
    }
}
