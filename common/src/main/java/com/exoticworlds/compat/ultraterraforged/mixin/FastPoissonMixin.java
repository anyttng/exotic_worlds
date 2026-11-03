package com.exoticworlds.compat.ultraterraforged.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.exoticworlds.compat.terraforged.RtfLap;
import com.exoticworlds.compat.ultraterraforged.UltraTerraForgedInjectionTargets;

import com.pandaismyname1.ultraterraforged.world.worldgen.feature.placement.poisson.FastPoisson;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.placement.poisson.FastPoissonContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;

@Mixin(value = FastPoisson.class, remap = false)
public abstract class FastPoissonMixin {
    @Unique
    private static final String CONTEXT_FREQUENCY = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/feature/"
            + "placement/poisson/FastPoissonContext;frequency()F";

    @Unique
    private static final String CONTEXT_SCALE = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/feature/"
            + "placement/poisson/FastPoissonContext;scale()F";

    @Unique
    private static final String FRAME = "poissonFrame";

    @Unique
    private static final String X_SCALE = "poissonXScale";

    @Unique
    private static final String Z_SCALE = "poissonZScale";

    @Unique
    private static final String X_LAPS = "poissonXLaps";

    @Unique
    private static final String Z_LAPS = "poissonZLaps";

    @Unique
    private static final String POINT_X = "poissonPointX";

    @Unique
    private static final String POINT_Z = "poissonPointZ";

    @Unique
    private static final int X_READ = 0;

    @Unique
    private static final int Z_READ = 1;

    @Unique
    private static final int POINT_X_FLOOR = 2;

    @Unique
    private static final int POINT_Z_FLOOR = 3;

    @WrapMethod(method = "getPoint")
    private static long exotic_worlds$closeOnLap(int seed, float x, float z, FastPoissonContext context,
            Operation<Long> original, @Share(FRAME) LocalRef<RtfLap.@Nullable Frame> lap,
            @Share(X_SCALE) LocalFloatRef xScale, @Share(Z_SCALE) LocalFloatRef zScale,
            @Share(X_LAPS) LocalIntRef xLaps, @Share(Z_LAPS) LocalIntRef zLaps) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        lap.set(frame);
        if (frame == null) {
            return original.call(seed, x, z, context);
        }

        try (RtfLap.Frame.Scope lattice = frame.octave(context.frequency())) {
            xScale.set(frame.xScale());
            zScale.set(frame.zScale());
            float latticeX = frame.latticeX(x, z);
            float latticeZ = frame.shiftZ(z);
            xLaps.set(Math.round(x - frame.outerX(latticeX, latticeZ)));
            zLaps.set(Math.round(z - latticeZ));
            return original.call(seed, latticeX, latticeZ, context);
        }
    }

    @ModifyExpressionValue(method = "getPoint",
            at = @At(value = "INVOKE", target = CONTEXT_FREQUENCY, ordinal = X_READ))
    private static float exotic_worlds$snapX(float frequency, @Share(FRAME) LocalRef<RtfLap.@Nullable Frame> lap,
            @Share(X_SCALE) LocalFloatRef xScale) {
        return lap.get() == null ? frequency : xScale.get();
    }

    @ModifyExpressionValue(method = "getPoint",
            at = @At(value = "INVOKE", target = CONTEXT_FREQUENCY, ordinal = Z_READ))
    private static float exotic_worlds$snapZ(float frequency, @Share(FRAME) LocalRef<RtfLap.@Nullable Frame> lap,
            @Share(Z_SCALE) LocalFloatRef zScale) {
        return lap.get() == null ? frequency : zScale.get();
    }

    @ModifyExpressionValue(method = "getPoint", at = @At(value = "INVOKE", target = CONTEXT_SCALE, ordinal = X_READ))
    private static float exotic_worlds$snapScaleX(float scale, @Share(FRAME) LocalRef<RtfLap.@Nullable Frame> lap,
            @Share(X_SCALE) LocalFloatRef xScale) {
        return lap.get() == null ? scale : 1.0F / xScale.get();
    }

    @ModifyExpressionValue(method = "getPoint", at = @At(value = "INVOKE", target = CONTEXT_SCALE, ordinal = Z_READ))
    private static float exotic_worlds$snapScaleZ(float scale, @Share(FRAME) LocalRef<RtfLap.@Nullable Frame> lap,
            @Share(Z_SCALE) LocalFloatRef zScale) {
        return lap.get() == null ? scale : 1.0F / zScale.get();
    }

    @WrapOperation(method = "getPoint",
            at = @At(value = "INVOKE", target = UltraTerraForgedInjectionTargets.NOISE_FLOOR, ordinal = POINT_X_FLOOR))
    private static int exotic_worlds$keepPointX(float value, Operation<Integer> original,
            @Share(POINT_X) LocalFloatRef pointX) {
        pointX.set(value);
        return original.call(value);
    }

    @WrapOperation(method = "getPoint",
            at = @At(value = "INVOKE", target = UltraTerraForgedInjectionTargets.NOISE_FLOOR, ordinal = POINT_Z_FLOOR))
    private static int exotic_worlds$keepPointZ(float value, Operation<Integer> original,
            @Share(POINT_Z) LocalFloatRef pointZ) {
        pointZ.set(value);
        return original.call(value);
    }

    @WrapOperation(method = "getPoint", at = @At(value = "INVOKE", target = UltraTerraForgedInjectionTargets.POS_PACK))
    private static long exotic_worlds$moveByLaps(int x, int z, Operation<Long> original,
            @Share(FRAME) LocalRef<RtfLap.@Nullable Frame> lap, @Share(POINT_X) LocalFloatRef pointX,
            @Share(POINT_Z) LocalFloatRef pointZ, @Share(X_LAPS) LocalIntRef xLaps, @Share(Z_LAPS) LocalIntRef zLaps) {
        RtfLap.Frame frame = lap.get();
        if (frame == null) {
            return original.call(x, z);
        }

        int outerX = frame.sheared() ? NoiseUtil.floor(frame.outerX(pointX.get(), pointZ.get())) : x;
        return original.call(outerX + xLaps.get(), z + zLaps.get());
    }
}
