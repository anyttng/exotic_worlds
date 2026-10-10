package com.exoticworlds.engine.noise;

import com.exoticworlds.accessors.CoastLiftCache;
import com.exoticworlds.accessors.IndexableNoise;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationTransformerContext.Context;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class ContextScaledNoise {
    public static double sample(Context context, DensityFunction.NoiseHolder noise,
            double x, double y, double z, double horizontalScale) {
        try (Context.ScaleScope _ = context.withScale(horizontalScale)) {
            return noise.getValue(x, y, z);
        }
    }

    public static double sample(Context context, DensityFunction.NoiseHolder noise,
            double x, double y, double z, double horizontalScale, double verticalShare) {
        try (Context.ScaleScope _ = context.withScale(horizontalScale, verticalShare)) {
            return noise.getValue(x, y, z);
        }
    }

    public static double sample(Context context, DensityFunction.NoiseHolder noise,
            double x, double y, double z, double horizontalScale, double vanillaScale, double verticalShare) {
        try (Context.ScaleScope _ = context.withScales(horizontalScale, vanillaScale, verticalShare)) {
            return noise.getValue(x, y, z);
        }
    }

    public static double sample(Context context, NormalNoise noise,
            double x, double y, double z, double horizontalScale) {
        try (Context.ScaleScope _ = context.withScale(horizontalScale)) {
            return noise.getValue(x, y, z);
        }
    }

    public static double sampleWarped(Context context, DensityFunction.NoiseHolder noise,
            int blockX, double y, int blockZ, double shiftX, double shiftZ, double warpDivisor,
            double horizontalScale, double vanillaScale, double verticalShare) {
        try (Context.ScaleScope _ = context.withScales(horizontalScale, vanillaScale, verticalShare)) {
            return warped(context, noise, blockX, y, blockZ, shiftX, shiftZ, warpDivisor);
        }
    }

    // Bound rather than read: a sample on a thread nothing bound would otherwise write vanilla terrain to disk.
    public static double sampleWrapped(WorldFold transformer, SlotAxes axes,
            DensityFunction.NoiseHolder noise,
            double x, double y, double z, double horizontalScale, double vanillaScale, double verticalShare) {
        Context context = GenerationTransformerContext.context();

        try (Context.BindingScope _ = context.bind(transformer, axes, horizontalScale, vanillaScale,
                verticalShare)) {
            return noise.getValue(x, y, z);
        }
    }

    public static double sampleWrappedWarped(WorldFold transformer, DensityFunction.NoiseHolder noise,
            int blockX, double y, int blockZ, double shiftX, double shiftZ, double warpDivisor,
            double horizontalScale, double vanillaScale, double verticalShare) {
        Context context = GenerationTransformerContext.context();

        try (Context.BindingScope _ = context.bind(transformer, SlotAxes.DEFAULT, horizontalScale, vanillaScale,
                verticalShare)) {
            return warped(context, noise, blockX, y, blockZ, shiftX, shiftZ, warpDivisor);
        }
    }

    public static double vanillaAtFoldedSlots(Context context, NormalNoise noise, double x, double y, double z) {
        SlotAxes axes = context.slotAxes();
        TranslationLattice lattice = context.transformer().blockLattice();
        double worldZ = worldCoord(axes, SlotAxis.Z, x, y, z);
        double foldedX = lattice.foldX(worldCoord(axes, SlotAxis.X, x, y, z), worldZ);
        double foldedZ = lattice.foldZ(worldZ);
        double vanillaX = vanillaSlot(context, axes.x(), x, foldedX, foldedZ);
        double vanillaY = vanillaSlot(context, axes.y(), y, foldedX, foldedZ);
        double vanillaZ = vanillaSlot(context, axes.z(), z, foldedX, foldedZ);

        try (Context.BindingScope _ = context.unbound()) {
            return noise.getValue(vanillaX, vanillaY, vanillaZ);
        }
    }

    private static double warped(Context context, DensityFunction.NoiseHolder noise,
            int blockX, double y, int blockZ, double shiftX, double shiftZ, double warpDivisor) {
        TranslationLattice lattice = context.transformer().blockLattice();
        double vanillaScale = context.vanillaScale();
        if (!indexable(context, noise) || !DomainWarp.carries(lattice, blockX, blockZ, shiftX, shiftZ, vanillaScale)) {
            return vanillaAt(context, noise, lattice.foldX(blockX, blockZ) * vanillaScale + shiftX, y,
                    lattice.foldZ(blockZ) * vanillaScale + shiftZ);
        }

        return noise.getValue(DomainWarp.applyX(lattice, blockX, blockZ, shiftX, warpDivisor), y,
                DomainWarp.applyZ(lattice, blockZ, shiftZ, warpDivisor));
    }

    private static boolean indexable(Context context, DensityFunction.NoiseHolder noise) {
        return !(noise.noise() instanceof IndexableNoise indexable) || indexable.toroidal$indexable(context);
    }

    private static double vanillaAt(Context context, DensityFunction.NoiseHolder noise, double x, double y,
            double z) {
        double value;
        try (Context.BindingScope _ = context.unbound()) {
            value = noise.getValue(x, y, z);
        }

        return noise.noise() instanceof CoastLiftCache coast ? value + coast.toroidal$coastLift() : value;
    }

    private static double worldCoord(SlotAxes axes, SlotAxis axis, double x, double y, double z) {
        if (axes.x() == axis) {
            return x;
        }

        if (axes.y() == axis) {
            return y;
        }

        return axes.z() == axis ? z : 0.0;
    }

    private static double vanillaSlot(Context context, SlotAxis axis, double raw, double foldedX, double foldedZ) {
        return switch (axis) {
            case X -> foldedX * context.vanillaScale() / axis.divisorIn(context);
            case Z -> foldedZ * context.vanillaScale() / axis.divisorIn(context);
            case NONE -> raw;
        };
    }

    private ContextScaledNoise() {
    }
}
