package com.exoticworlds.compat.c2me;

import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.accessors.ScaledShiftedNoise;
import com.exoticworlds.compat.worldgen.ShiftedNoiseFold;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.DensityFunctionSlotAxes;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.NoiseConstants;
import com.exoticworlds.engine.noise.NoiseScaleLadder;
import com.exoticworlds.engine.noise.SlotAxes;
import com.exoticworlds.engine.noise.SlotAxis;
import com.exoticworlds.shape.climate.ClimateCompression;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.AddNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MulNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CoordinateNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.DelegateNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.GenericShiftedNoiseNode;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

public final class C2meDfcAst {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String GAME_PACKAGE = "net.minecraft.";

    private static final Set<String> REPORTED_MISSES = new HashSet<>();

    private static @Nullable WorldFold reportedMissesFor;

    public static AstNode fold(DensityFunction source, AstNode produced) {
        WorldFold worldFold = GenerationFoldContext.context().routerBuildFold();
        if (source instanceof DensityFunctions.WeirdScaledSampler) {
            return worldFold != null ? new DelegateNode(source) : produced;
        }

        if (source instanceof ScaledShiftedNoise scaled) {
            return worldFold == null ? produced : foldScaledShifted(source, scaled, produced, worldFold);
        }

        Fold fold = foldOf(source);
        if (fold == null) {
            if (reportsMiss(source, produced, worldFold)) {
                LOGGER.warn("[ew/c2me-compat] dfc_ast noise_not_folded type={}", source.getClass().getName());
            }

            return produced;
        }

        if (worldFold == null) {
            return produced;
        }

        if (fold.amplified()) {
            if (!(produced instanceof MulNode amplified && amplified.left instanceof GenericShiftedNoiseNode noise)) {
                throw brokenShape(source, produced);
            }

            return new MulNode(foldNoise(source, noise, fold, worldFold), amplified.right);
        }

        if (!(produced instanceof GenericShiftedNoiseNode noise)) {
            throw brokenShape(source, produced);
        }

        return foldNoise(source, noise, fold, worldFold);
    }

    private static @Nullable Fold foldOf(DensityFunction source) {
        return switch (source) {
            case DensityFunctions.Noise noise -> noiseFold(noise);
            case DensityFunctions.ShiftedNoise shifted -> shiftedNoiseFold(shifted);
            case DensityFunctions.Shift _, DensityFunctions.ShiftA _ -> new Fold(SlotAxes.DEFAULT,
                    NoiseConstants.SHIFT_SCALE, NoiseConstants.SHIFT_SCALE,
                    GenerationFoldContext.UNDECLARED_VERTICAL_SHARE, true, false);
            case DensityFunctions.ShiftB _ -> new Fold(DensityFunctionSlotAxes.SHIFT_B, NoiseConstants.SHIFT_SCALE,
                    NoiseConstants.SHIFT_SCALE, GenerationFoldContext.UNDECLARED_VERTICAL_SHARE, true, false);
            default -> null;
        };
    }

    private static boolean delegatesToItsInput(DensityFunction source) {
        return source instanceof DensityFunctions.HolderHolder;
    }

    static boolean reportsMiss(DensityFunction source, AstNode produced, @Nullable WorldFold worldFold) {
        if (worldFold == null
                || delegatesToItsInput(source)
                || !holdsUnfoldedNoise(produced, isForeign(source))) {
            return false;
        }

        return firstMissOf(worldFold, source.getClass().getName());
    }

    private static boolean isForeign(DensityFunction source) {
        return !source.getClass().getName().startsWith(GAME_PACKAGE);
    }

    private static boolean holdsUnfoldedNoise(AstNode node, boolean searchesChildren) {
        if (node instanceof GenericShiftedNoiseNode) {
            return !(node instanceof C2meFoldedNoiseNode);
        }

        if (!searchesChildren) {
            return false;
        }

        for (AstNode child : node.getChildren()) {
            if (holdsUnfoldedNoise(child, true)) {
                return true;
            }
        }

        return false;
    }

    private static synchronized boolean firstMissOf(WorldFold worldFold, String type) {
        if (worldFold != reportedMissesFor) {
            REPORTED_MISSES.clear();
            reportedMissesFor = worldFold;
        }

        return REPORTED_MISSES.add(type);
    }

    @SuppressWarnings("deprecation")
    private static Fold noiseFold(DensityFunctions.Noise noise) {
        double xzScale = NoiseScaleLadder.installedScale(noise.noise(), noise.xzScale());
        return new Fold(SlotAxes.DEFAULT, xzScale, noise.xzScale(),
                GenerationFoldContext.verticalShare(xzScale, noise.yScale()), false, false);
    }

    private static Fold shiftedNoiseFold(DensityFunctions.ShiftedNoise shifted) {
        double xzScale = NoiseScaleLadder.installedScale(shifted.noise(), shifted.xzScale());
        return new Fold(SlotAxes.DEFAULT, xzScale, shifted.xzScale(),
                GenerationFoldContext.verticalShare(xzScale, shifted.yScale()), false, xzScale != 0.0);
    }

    private static AstNode foldNoise(DensityFunction source, GenericShiftedNoiseNode noise, Fold fold,
            WorldFold worldFold) {
        SlotAxes axes = fold.axes();
        AstNode slotY = slotNode(axes.y(), noise.inputY);
        if (fold.warped()) {
            return new C2meFoldedNoiseNode(noise.inputX, noise.inputY, noise.inputZ, noise.noise,
                    shiftOf(source, CoordinateNode.Axis.X, noise.inputX), slotY,
                    shiftOf(source, CoordinateNode.Axis.Z, noise.inputZ), axes, fold.horizontalScale(),
                    fold.vanillaScale(), fold.verticalShare(), ClimateCompression.warpDivisor(noise.noise,
                            worldFold, fold.horizontalScale(), fold.verticalShare()), worldFold);
        }

        return new C2meFoldedNoiseNode(noise.inputX, noise.inputY, noise.inputZ, noise.noise,
                slotNode(axes.x(), noise.inputX), slotY, slotNode(axes.z(), noise.inputZ), axes,
                fold.horizontalScale(), fold.vanillaScale(), fold.verticalShare(), C2meFoldedNoiseNode.UNWARPED,
                worldFold);
    }

    private static AstNode foldScaledShifted(DensityFunction source, ScaledShiftedNoise scaled, AstNode produced,
            WorldFold worldFold) {
        if (!(produced instanceof AddNode offset
                && offset.left instanceof MulNode multiplied
                && multiplied.left instanceof GenericShiftedNoiseNode noise)) {
            throw brokenShape(source, produced);
        }

        ShiftedNoiseFold fold = ShiftedNoiseFold.of(scaled);
        boolean shiftsScaled = scaled.exotic_worlds$shiftsScaled();
        AstNode folded = new C2meFoldedNoiseNode(noise.inputX, noise.inputY, noise.inputZ, noise.noise,
                warpOf(source, shiftsScaled, CoordinateNode.Axis.X, noise.inputX, fold.shiftScale()),
                slotNode(SlotAxes.DEFAULT.y(), noise.inputY),
                warpOf(source, shiftsScaled, CoordinateNode.Axis.Z, noise.inputZ, fold.shiftScale()),
                SlotAxes.DEFAULT, fold.horizontalScale(), fold.vanillaScale(), fold.verticalShare(),
                fold.warpDivisor(worldFold), worldFold);
        return new AddNode(new MulNode(folded, multiplied.right), offset.right);
    }

    private static AstNode warpOf(DensityFunction source, boolean shiftsScaled, CoordinateNode.Axis axis,
            AstNode ownInput, double shiftScale) {
        if (!shiftsScaled) {
            return shiftOf(source, axis, ownInput);
        }

        if (!(ownInput instanceof MulNode scaled
                && scaled.left instanceof AddNode shifted
                && shifted.left instanceof CoordinateNode coordinate
                && coordinate.axis == axis)) {
            throw brokenShape(source, ownInput);
        }

        return new MulNode(shifted.right, new ConstantNode(shiftScale));
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
        return new IllegalStateException("[ew/c2me-compat] dfc_ast broken_shape type=" + source.getClass().getName()
                + " produced=" + produced.getClass().getName()
                + "; C2ME no longer compiles this function to the node the toroidal fold replaces");
    }

    private record Fold(SlotAxes axes, double horizontalScale, double vanillaScale, double verticalShare,
            boolean amplified, boolean warped) {
    }

    private C2meDfcAst() {
    }
}
