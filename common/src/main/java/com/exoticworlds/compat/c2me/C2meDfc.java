package com.exoticworlds.compat.c2me;

import java.util.List;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;

public final class C2meDfc {
    static final List<String> MIXINS = List.of("McToAstMixin", "BytecodeGenRegistryMixin", "DotGenRegistryMixin");

    static final ModPresence GATE = ModPresence.gate(LogUtils.getLogger(), "[c2me-compat] gate dfc_present")
            .probing("com/ishland/c2me/opts/dfc/mixin/MixinNoiseConfig.class")
            .checking(C2meMixinPlugin.CONFIG, MIXINS::contains)
            .build();

    public static boolean present() {
        return GATE.present();
    }

    private C2meDfc() {
    }
}
