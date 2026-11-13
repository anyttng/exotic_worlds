package com.exoticworlds.compat.c2me;

import java.util.List;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.accessors.FoldableFastNoise;
import com.exoticworlds.compat.gate.ModPresence;
import com.exoticworlds.core.WorldFold;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;

public final class C2meFastNoise {
    private static final Logger LOGGER = LogUtils.getLogger();

    static final String FAST_NOISE_NODE =
            "com.ishland.c2me.opts.dfc.common.ast.integration.lithostitched.misc.GenericFastNoiseNode";

    static final List<String> MIXINS = List.of("FastNoiseBytecodeGenRegistryMixin", "FastNoiseDotGenRegistryMixin");
    static final List<String> CLASSES = List.of("C2meFastNoise", "C2meFoldedFastNoiseNode",
            "C2meFoldedFastNoiseEmitter", "C2meFoldedFastNoiseDotEmitter");

    static final ModPresence GATE = ModPresence.gate(LOGGER, "[ew/c2me-compat] gate dfc_fast_noise_present")
            .probing(FAST_NOISE_NODE.replace('.', '/') + ".class")
            .checking(C2meMixinPlugin.CONFIG, MIXINS::contains)
            .requiring(C2meDfc.GATE)
            .modPackages(C2meMixinPlugin.C2ME_PACKAGE, C2meMixinPlugin.FLOWSCHED_PACKAGE)
            .scanning(CLASSES::contains)
            .build();

    static AstNode fold(FoldableFastNoise source, AstNode produced, WorldFold worldFold) {
        return GATE.admitted() ? C2meFoldedFastNoiseNode.folding(source, produced, worldFold) : produced;
    }

    static boolean unfolded(AstNode node) {
        return FAST_NOISE_NODE.equals(node.getClass().getName());
    }

    private C2meFastNoise() {
    }
}
