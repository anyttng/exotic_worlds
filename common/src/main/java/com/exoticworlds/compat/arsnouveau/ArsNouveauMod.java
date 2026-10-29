package com.exoticworlds.compat.arsnouveau;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;

final class ArsNouveauMod {
    static final ModPresence GATE = ModPresence.gate(LogUtils.getLogger(), "[ew/ars-compat] gate ars_nouveau_present")
            .probing("com/hollingsworth/arsnouveau/ArsNouveau.class")
            .checking("exotic_worlds.compat.arsnouveau.mixins.json")
            .build();

    static boolean admitted() {
        return GATE.admitted();
    }

    private ArsNouveauMod() {
    }
}
