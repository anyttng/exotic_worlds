package com.exoticworlds.compat.c2me;

import java.util.Objects;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.SlotAxes;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.ast.noise.GenericShiftedNoiseNode;

import net.minecraft.world.level.levelgen.DensityFunction;

public final class C2meFoldedNoiseNode extends GenericShiftedNoiseNode {
    public static final double UNWARPED = 0.0;

    // On a warped node the X and Z slots carry the shift; the sampler adds it to the folded block coordinate.
    public final AstNode slotX;
    public final AstNode slotY;
    public final AstNode slotZ;

    public final SlotAxes slotAxes;

    public final double horizontalScale;

    public final double vanillaScale;

    public final double verticalShare;

    public final double warpDivisor;

    public final WorldFold transformer;

    public C2meFoldedNoiseNode(AstNode inputX, AstNode inputY, AstNode inputZ, DensityFunction.NoiseHolder noise,
            AstNode slotX, AstNode slotY, AstNode slotZ, SlotAxes slotAxes, double horizontalScale,
            double vanillaScale, double verticalShare, double warpDivisor, WorldFold transformer) {
        super(inputX, inputY, inputZ, noise);
        this.slotX = Objects.requireNonNull(slotX);
        this.slotY = Objects.requireNonNull(slotY);
        this.slotZ = Objects.requireNonNull(slotZ);
        this.slotAxes = Objects.requireNonNull(slotAxes);
        this.horizontalScale = horizontalScale;
        this.vanillaScale = vanillaScale;
        this.verticalShare = verticalShare;
        this.warpDivisor = warpDivisor;
        this.transformer = Objects.requireNonNull(transformer);
    }

    public boolean warped() {
        return this.warpDivisor != UNWARPED;
    }

    @Override
    public AstNode[] getChildren() {
        return new AstNode[]{this.inputX, this.inputY, this.inputZ, this.slotX, this.slotY, this.slotZ};
    }

    @Override
    public AstNode transform(AstTransformer transformer) {
        AstNode transformedInputX = this.inputX.transform(transformer);
        AstNode transformedInputY = this.inputY.transform(transformer);
        AstNode transformedInputZ = this.inputZ.transform(transformer);
        AstNode transformedSlotX = this.slotX.transform(transformer);
        AstNode transformedSlotY = this.slotY.transform(transformer);
        AstNode transformedSlotZ = this.slotZ.transform(transformer);
        boolean unchanged = transformedInputX == this.inputX
                && transformedInputY == this.inputY
                && transformedInputZ == this.inputZ
                && transformedSlotX == this.slotX
                && transformedSlotY == this.slotY
                && transformedSlotZ == this.slotZ;

        return transformer.transform(unchanged
                ? this
                : new C2meFoldedNoiseNode(transformedInputX, transformedInputY, transformedInputZ, this.noise,
                        transformedSlotX, transformedSlotY, transformedSlotZ, this.slotAxes, this.horizontalScale,
                        this.vanillaScale, this.verticalShare, this.warpDivisor, this.transformer));
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }

        C2meFoldedNoiseNode that = (C2meFoldedNoiseNode) o;
        return sameFields(that)
                && this.slotX.equals(that.slotX)
                && this.slotY.equals(that.slotY)
                && this.slotZ.equals(that.slotZ);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.slotX.hashCode();
        result = 31 * result + this.slotY.hashCode();
        result = 31 * result + this.slotZ.hashCode();
        return fieldHash(result);
    }

    @Override
    public boolean relaxedEquals(AstNode o) {
        if (!super.relaxedEquals(o)) {
            return false;
        }

        C2meFoldedNoiseNode that = (C2meFoldedNoiseNode) o;
        return sameFields(that)
                && this.slotX.relaxedEquals(that.slotX)
                && this.slotY.relaxedEquals(that.slotY)
                && this.slotZ.relaxedEquals(that.slotZ);
    }

    @Override
    public int relaxedHashCode() {
        int result = super.relaxedHashCode();
        result = 31 * result + this.slotX.relaxedHashCode();
        result = 31 * result + this.slotY.relaxedHashCode();
        result = 31 * result + this.slotZ.relaxedHashCode();
        return fieldHash(result);
    }

    private boolean sameFields(C2meFoldedNoiseNode that) {
        return Double.compare(this.horizontalScale, that.horizontalScale) == 0
                && Double.compare(this.vanillaScale, that.vanillaScale) == 0
                && Double.compare(this.verticalShare, that.verticalShare) == 0
                && Double.compare(this.warpDivisor, that.warpDivisor) == 0
                && this.transformer == that.transformer
                && this.slotAxes.equals(that.slotAxes);
    }

    private int fieldHash(int seed) {
        int result = 31 * seed + this.slotAxes.hashCode();
        result = 31 * result + Double.hashCode(this.horizontalScale);
        result = 31 * result + Double.hashCode(this.vanillaScale);
        result = 31 * result + Double.hashCode(this.verticalShare);
        result = 31 * result + Double.hashCode(this.warpDivisor);
        return 31 * result + System.identityHashCode(this.transformer);
    }
}
