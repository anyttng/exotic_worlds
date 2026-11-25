package com.exoticworlds.compat.apprenticecodex;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.gate.ModPresence;

final class ApprenticeCodexMod {
    private static final String APPRENTICE_CODEX_PACKAGE = "jp/aquafactory/apprenticecodex/";

    static final ModPresence GATE = ModPresence.gate(LogUtils.getLogger(),
                    "[ew/apprentice-compat] gate apprenticecodex_present")
            .probing("jp/aquafactory/apprenticecodex/ApprenticeCodex.class")
            .checking("exotic_worlds.compat.apprenticecodex.mixins.json")
            .modPackages(APPRENTICE_CODEX_PACKAGE)
            .build();

    static boolean admitted() {
        return GATE.admitted();
    }

    private ApprenticeCodexMod() {
    }
}
