package com.exoticworlds.compat.c2me;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.ContextScaledNoise;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.GenerationFoldContext.Context;
import com.exoticworlds.engine.noise.SlotAxes;

import net.minecraft.world.level.levelgen.DensityFunction;

public final class C2meFoldedNoiseLoop {
    // A null axis array means C2ME const-eliminated that input; its value is the paired constant.
    public static void fill(WorldFold fold, SlotAxes axes, DensityFunction.NoiseHolder noise,
            double[] res, double @Nullable [] xs, double xConst, double @Nullable [] ys, double yConst,
            double @Nullable [] zs, double zConst, double horizontalScale, double vanillaScale,
            double verticalShare) {
        Context context = GenerationFoldContext.context();

        try (Context.BindingScope _ = context.bind(fold, axes, horizontalScale, vanillaScale,
                verticalShare)) {
            for (int i = 0; i < res.length; i++) {
                double x = xs != null ? xs[i] : xConst;
                double y = ys != null ? ys[i] : yConst;
                double z = zs != null ? zs[i] : zConst;
                res[i] = ContextScaledNoise.wrappedValue(context, noise, x, y, z);
            }
        }
    }

    public static void fillWarped(WorldFold fold, DensityFunction.NoiseHolder noise, double[] res,
            int[] blockXs, int[] blockZs, double @Nullable [] shiftXs, double shiftXConst,
            double @Nullable [] ys, double yConst, double @Nullable [] shiftZs, double shiftZConst,
            double warpDivisor, double horizontalScale, double vanillaScale, double verticalShare) {
        Context context = GenerationFoldContext.context();
        for (int i = 0; i < res.length; i++) {
            double shiftX = shiftXs != null ? shiftXs[i] : shiftXConst;
            double y = ys != null ? ys[i] : yConst;
            double shiftZ = shiftZs != null ? shiftZs[i] : shiftZConst;
            res[i] = ContextScaledNoise.sampleWrappedWarped(context, fold, noise, blockXs[i], y, blockZs[i],
                    shiftX, shiftZ, warpDivisor, horizontalScale, vanillaScale, verticalShare);
        }
    }

    private C2meFoldedNoiseLoop() {
    }
}
