package com.exoticworlds.compat.ironsspellbooks;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.gate.ModPresence;

final class IronsSpellbooksMod {
    private static final String IRONS_SPELLBOOKS_PACKAGE = "io/redspace/ironsspellbooks/";

    static final ModPresence GATE = ModPresence.gate(LogUtils.getLogger(),
                    "[ew/irons-compat] gate irons_spellbooks_present")
            .probing("io/redspace/ironsspellbooks/IronsSpellbooks.class")
            .checking("exotic_worlds.compat.ironsspellbooks.mixins.json")
            .modPackages(IRONS_SPELLBOOKS_PACKAGE)
            .build();

    static boolean admitted() {
        return GATE.admitted();
    }

    private IronsSpellbooksMod() {
    }
}
