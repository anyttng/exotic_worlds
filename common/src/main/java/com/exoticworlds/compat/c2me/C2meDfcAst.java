package com.exoticworlds.compat.c2me;

import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.DensityFunctionSlotAxes;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.exoticworlds.engine.noise.NoiseConstants;
import com.exoticworlds.engine.noise.NoiseScaleLadder;
import com.exoticworlds.engine.noise.SlotAxes;
import com.exoticworlds.engine.noise.SlotAxis;
import com.exoticworlds.shape.climate.ClimateCompression;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.AddNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MulNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CoordinateNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.GenericShiftedNoiseNode;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

public final class C2meDfcAst {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Set<String> REPORTED_MISSES = new HashSet<>();

    private static @Nullable WorldFold reportedMissesFor;

    public static AstNode fold(DensityFunction source, AstNode produced) {
        WorldFold transformer = GenerationTransformerContext.context().routerBuildTransformer();
        Fold fold = foldOf(source);
        if (fold == null) {
            if (reportsMiss(source, produced, transformer)) {
                LOGGER.warn("[c2me-compat] dfc_ast noise_not_folded type={}", source.getClass().getName());
            }

            return produced;
        }

        if (transformer == null) {
            return produced;
        }

        if (fold.amplified()) {
            if (!(produced instanceof MulNode amplified && amplified.left instanceof GenericShiftedNoiseNode noise)) {
                throw brokenShape(source, produced);
            }

            return new MulNode(foldNoise(source, noise, fold, transformer), amplified.right);
        }

        if (!(produced instanceof GenericShiftedNoiseNode noise)) {
            throw brokenShape(source, produced);
        }

        return foldNoise(source, noise, fold, transformer);
    }

    private static @Nullable Fold foldOf(DensityFunction source) {
        return switch (source) {
            case DensityFunctions.Noise noise -> noiseFold(noise);
            case DensityFunctions.ShiftedNoise shifted -> shiftedNoiseFold(shifted);
            case DensityFunctions.Shift _, DensityFunctions.ShiftA _ -> new Fold(SlotAxes.DEFAULT,
                    NoiseConstants.SHIFT_SCALE, NoiseConstants.SHIFT_SCALE,
                    GenerationTransformerContext.UNDECLARED_VERTICAL_SHARE, true, false);
            case DensityFunctions.ShiftB _ -> new Fold(DensityFunctionSlotAxes.SHIFT_B, NoiseConstants.SHIFT_SCALE,
                    NoiseConstants.SHIFT_SCALE, GenerationTransformerContext.UNDECLARED_VERTICAL_SHARE, true, false);
            default -> null;
        };
    }

    private static boolean delegatesToItsInput(DensityFunction source) {
        return source instanceof DensityFunctions.HolderHolder;
    }

    static boolean reportsMiss(DensityFunction source, AstNode produced, @Nullable WorldFold transformer) {
        if (transformer == null
                || !(produced instanceof GenericShiftedNoiseNode)
                || produced instanceof C2meFoldedNoiseNode
                || delegatesToItsInput(source)) {
            return false;
        }

        return firstMissOf(transformer, source.getClass().getName());
    }

    private static synchronized boolean firstMissOf(WorldFold transformer, String type) {
        if (transformer != reportedMissesFor) {
            REPORTED_MISSES.clear();
            reportedMissesFor = transformer;
        }

        return REPORTED_MISSES.add(type);
    }

    @SuppressWarnings("deprecation")
    private static Fold noiseFold(DensityFunctions.Noise noise) {
        double xzScale = NoiseScaleLadder.installedScale(noise.noise(), noise.xzScale());
        return new Fold(SlotAxes.DEFAULT, xzScale, noise.xzScale(),
                GenerationTransformerContext.verticalShare(xzScale, noise.yScale()), false, false);
    }

    private static Fold shiftedNoiseFold(DensityFunctions.ShiftedNoise shifted) {
        double xzScale = NoiseScaleLadder.installedScale(shifted.noise(), shifted.xzScale());
        return new Fold(SlotAxes.DEFAULT, xzScale, shifted.xzScale(),
                GenerationTransformerContext.verticalShare(xzScale, shifted.yScale()), false, xzScale != 0.0);
    }

    private static AstNode foldNoise(DensityFunction source, GenericShiftedNoiseNode noise, Fold fold,
            WorldFold transformer) {
        SlotAxes axes = fold.axes();
        AstNode slotY = slotNode(axes.y(), noise.inputY);
        if (fold.warped()) {
            return new C2meFoldedNoiseNode(noise.inputX, noise.inputY, noise.inputZ, noise.noise,
                    shiftOf(source, CoordinateNode.Axis.X, noise.inputX), slotY,
                    shiftOf(source, CoordinateNode.Axis.Z, noise.inputZ), axes, fold.horizontalScale(),
                    fold.vanillaScale(), fold.verticalShare(), ClimateCompression.warpDivisor(noise.noise,
                            transformer, fold.horizontalScale(), fold.verticalShare()), transformer);
        }

        return new C2meFoldedNoiseNode(noise.inputX, noise.inputY, noise.inputZ, noise.noise,
                slotNode(axes.x(), noise.inputX), slotY, slotNode(axes.z(), noise.inputZ), axes,
                fold.horizontalScale(), fold.vanillaScale(), fold.verticalShare(), C2meFoldedNoiseNode.UNWARPED,
                transformer);
    }

    private static AstNode slotNode(SlotAxis axis, AstNode ownInput) {
        return switch (axis) {
            case X -> CoordinateNode.AXIS_X;
            case Z -> CoordinateNode.AXIS_Z;
            case NONE -> ownInput;
        };
    }

    private static AstNode shiftOf(DensityFunction source, CoordinateNode.Axis axis, AstNode ownInput) {
        if (!(ownInput instanceof AddNode shifted
                && shifted.left instanceof MulNode scaled
                && scaled.left instanceof CoordinateNode coordinate
                && coordinate.axis == axis)) {
            throw brokenShape(source, ownInput);
        }

        return shifted.right;
    }

    private static IllegalStateException brokenShape(DensityFunction source, AstNode produced) {
        return new IllegalStateException("[c2me-compat] dfc_ast broken_shape type=" + source.getClass().getName()
                + " produced=" + produced.getClass().getName()
                + " — C2ME no longer compiles this function to the node the toroidal fold replaces");
    }

    private record Fold(SlotAxes axes, double horizontalScale, double vanillaScale, double verticalShare,
            boolean amplified, boolean warped) {
    }

    private C2meDfcAst() {
    }
}
