package com.exoticworlds.compat.c2me;

import java.util.Objects;

import com.exoticworlds.accessors.FoldableFastNoise;
import com.exoticworlds.core.WorldFold;
import com.ishland.c2me.base.common.integration.lithostitched.FNLBindings;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.ast.integration.lithostitched.misc.GenericFastNoiseNode;

public final class C2meFoldedFastNoiseNode extends GenericFastNoiseNode {
    public final WorldFold worldFold;

    public C2meFoldedFastNoiseNode(AstNode inputX, AstNode inputY, AstNode inputZ, FNLBindings.FNLState state,
            Object config, WorldFold worldFold) {
        super(inputX, inputY, inputZ, state, config);
        this.worldFold = Objects.requireNonNull(worldFold);
    }

    static AstNode folding(FoldableFastNoise source, AstNode produced, WorldFold worldFold) {
        if (produced == null || produced.getClass() != GenericFastNoiseNode.class) {
            return produced;
        }

        Object config = source.exotic_worlds$closedConfig(worldFold);
        if (config == null) {
            return produced;
        }

        GenericFastNoiseNode noise = (GenericFastNoiseNode) produced;
        return new C2meFoldedFastNoiseNode(noise.inputX, noise.inputY, noise.inputZ, noise.state, config, worldFold);
    }

    @Override
    public AstNode transform(AstTransformer transformer) {
        AstNode transformedInputX = this.inputX.transform(transformer);
        AstNode transformedInputY = this.inputY.transform(transformer);
        AstNode transformedInputZ = this.inputZ.transform(transformer);
        boolean unchanged = transformedInputX == this.inputX
                && transformedInputY == this.inputY
                && transformedInputZ == this.inputZ;

        return transformer.transform(unchanged
                ? this
                : new C2meFoldedFastNoiseNode(transformedInputX, transformedInputY, transformedInputZ, this.state,
                        this.config, this.worldFold));
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && this.worldFold == ((C2meFoldedFastNoiseNode) o).worldFold;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + System.identityHashCode(this.worldFold);
    }

    @Override
    public boolean relaxedEquals(AstNode o) {
        return super.relaxedEquals(o) && this.worldFold == ((C2meFoldedFastNoiseNode) o).worldFold;
    }

    @Override
    public int relaxedHashCode() {
        return 31 * super.relaxedHashCode() + System.identityHashCode(this.worldFold);
    }
}
