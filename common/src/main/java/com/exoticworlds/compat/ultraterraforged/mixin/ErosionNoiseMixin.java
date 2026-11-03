package com.exoticworlds.compat.ultraterraforged.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.exoticworlds.compat.terraforged.CellCenters;
import com.exoticworlds.compat.terraforged.LapOctaves;
import com.exoticworlds.compat.terraforged.RtfLap;
import com.exoticworlds.compat.ultraterraforged.UltraTerraForgedNoise;

import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Erosion;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;

@Mixin(value = Erosion.class, remap = false)
public abstract class ErosionNoiseMixin {
    @Unique
    private static final int GRID_SIZE_ORDINAL = 2;

    @Unique
    private static final String SINGLE_EROSION_VALUE =
            "Lcom/pandaismyname1/ultraterraforged/world/worldgen/noise/module/Erosion;getSingleErosionValue(FFF[F)F";

    @Unique
    private static final double FIRST_OCTAVE_SCALE = 1.0;

    @Unique
    private static final float WHOLE_JITTER = 1.0F;

    @Shadow
    @Final
    private int seed;

    @Shadow
    @Final
    private float lacunarity;

    @WrapMethod(method = "getErosionValue")
    private float exotic_worlds$closeOnLap(float x, float y, float[] cache, Operation<Float> original,
            @Share(LapOctaves.FRAME) LocalRef<RtfLap.@Nullable Frame> frame,
            @Share(LapOctaves.BLOCK_X) LocalFloatRef blockX, @Share(LapOctaves.BLOCK_Z) LocalFloatRef blockZ,
            @Share(LapOctaves.FREQUENCY) LocalDoubleRef octaveScale) {
        LapOctaves.open(frame, blockX, blockZ, octaveScale, x, y, FIRST_OCTAVE_SCALE);
        return original.call(x, y, cache);
    }

    @WrapOperation(method = "getErosionValue", at = @At(value = "INVOKE", target = SINGLE_EROSION_VALUE))
    private float exotic_worlds$octaveOnLap(Erosion erosion, float x, float y, float distance, float[] cache,
            Operation<Float> original, @Share(LapOctaves.FRAME) LocalRef<RtfLap.@Nullable Frame> frame,
            @Share(LapOctaves.BLOCK_X) LocalFloatRef blockX, @Share(LapOctaves.BLOCK_Z) LocalFloatRef blockZ,
            @Share(LapOctaves.FREQUENCY) LocalDoubleRef octaveScale) {
        RtfLap.Frame lap = frame.get();
        if (lap == null) {
            return original.call(erosion, x, y, distance, cache);
        }

        double scale = octaveScale.get();
        octaveScale.set(scale * this.lacunarity);
        try (RtfLap.Frame.Scope _ = lap.octave(scale / distance)) {
            return original.call(erosion, lap.latticeU(blockX.get(), blockZ.get()) * distance,
                    lap.latticeV(blockZ.get()) * distance, distance, cache);
        }
    }

    // A candidate is a cell's jittered point, well inside the cell, so flooring it recovers the cell's index.
    @WrapOperation(method = "getSingleErosionValue", at = @At(value = "INVOKE",
            target = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/noise/module/Erosion;getNoiseValue(IIFF"
                    + "Lcom/pandaismyname1/ultraterraforged/world/worldgen/noise/module/Noise;[F)F"))
    private float exotic_worlds$canonicalCandidate(int dx, int dy, float px, float py, Noise module, float[] cache,
            Operation<Float> original, @Local(argsOnly = true, ordinal = GRID_SIZE_ORDINAL) float cellSize) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(dx, dy, px, py, module, cache);
        }

        int cellX = NoiseUtil.floor(px / cellSize);
        int cellY = NoiseUtil.floor(py / cellSize);
        float candidateX = CellCenters.jitteredX(UltraTerraForgedNoise.INSTANCE, frame, this.seed, cellX, cellY,
                WHOLE_JITTER) * cellSize;
        float candidateY = CellCenters.jitteredZ(UltraTerraForgedNoise.INSTANCE, frame, this.seed, cellX, cellY,
                WHOLE_JITTER) * cellSize;
        try (RtfLap.Frame.Scope _ = frame.open()) {
            return original.call(dx, dy, candidateX, candidateY, module, cache);
        }
    }
}
