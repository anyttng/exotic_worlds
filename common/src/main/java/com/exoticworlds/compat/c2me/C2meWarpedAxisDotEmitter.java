package com.exoticworlds.compat.c2me;

import com.ishland.c2me.opts.dfc.common.gen.dot.DotEmitter;
import com.ishland.c2me.opts.dfc.common.gen.dot.DotGen;

public final class C2meWarpedAxisDotEmitter implements DotEmitter<C2meWarpedAxisNode> {
    public static final C2meWarpedAxisDotEmitter INSTANCE = new C2meWarpedAxisDotEmitter();

    private C2meWarpedAxisDotEmitter() {
    }

    @Override
    public int doDotGen(C2meWarpedAxisNode node, DotGen.Context context, DotGen.Context.Builder builder) {
        return builder.hexagonShape()
                .label("ToroidalWarpedAxis\\naxis=" + node.axis + "\\ndivisor=" + node.divisor
                        + "\\nx=" + node.lattice.x().lowerBound + ".." + node.lattice.x().upperBound
                        + "\\nz=" + node.lattice.z().lowerBound + ".." + node.lattice.z().upperBound
                        + "\\nskew=" + node.lattice.skew())
                .edge(context.generate(node.shift))
                .label("shift")
                .finish()
                .build();
    }
}
