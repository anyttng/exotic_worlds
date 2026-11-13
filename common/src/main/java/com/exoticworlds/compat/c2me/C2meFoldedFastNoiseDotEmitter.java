package com.exoticworlds.compat.c2me;

import com.ishland.c2me.opts.dfc.common.gen.dot.DotEmitter;
import com.ishland.c2me.opts.dfc.common.gen.dot.DotGen;

public final class C2meFoldedFastNoiseDotEmitter implements DotEmitter<C2meFoldedFastNoiseNode> {
    public static final C2meFoldedFastNoiseDotEmitter INSTANCE = new C2meFoldedFastNoiseDotEmitter();

    private C2meFoldedFastNoiseDotEmitter() {
    }

    @Override
    public int doDotGen(C2meFoldedFastNoiseNode node, DotGen.Context context, DotGen.Context.Builder builder) {
        return builder.hexagonShape()
                .label("ToroidalFoldedFastNoise")
                .edge(context.generate(node.inputX))
                .label("inputX")
                .finish()
                .edge(context.generate(node.inputY))
                .label("inputY")
                .finish()
                .edge(context.generate(node.inputZ))
                .label("inputZ")
                .finish()
                .build();
    }
}
