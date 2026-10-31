package com.exoticworlds.compat.c2me;

import java.util.List;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;

public final class C2meDfc {
    static final List<String> MIXINS = List.of("McToAstMixin", "BytecodeGenRegistryMixin", "DotGenRegistryMixin");
    static final List<String> CLASSES = List.of("C2meDfc", "C2meDfcAst", "C2meFoldedNoiseDotEmitter",
            "C2meFoldedNoiseEmitter", "C2meFoldedNoiseLoop", "C2meFoldedNoiseNode");

    static final ModPresence GATE = ModPresence.gate(LogUtils.getLogger(), "[ew/c2me-compat] gate dfc_present")
            .probing("com/ishland/c2me/opts/dfc/mixin/MixinNoiseConfig.class")
            .checking(C2meMixinPlugin.CONFIG, MIXINS::contains)
            .modPackages(C2meMixinPlugin.C2ME_PACKAGE, C2meMixinPlugin.FLOWSCHED_PACKAGE)
            .scanning(CLASSES::contains)
            .build();

    private C2meDfc() {
    }
}
