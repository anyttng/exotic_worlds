package com.exoticworlds.compat.reterraforged.mixin;

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
import com.exoticworlds.compat.terraforged.LapOctaves;
import com.exoticworlds.compat.terraforged.RtfLap;

import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Erosion;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;

@Mixin(value = Erosion.class, remap = false)
public abstract class ErosionNoiseMixin {
    @Unique
    private static final int GRID_SIZE_ORDINAL = 2;

    @Unique
    private static final String SINGLE_EROSION_VALUE =
            "Lraccoonman/reterraforged/world/worldgen/noise/module/Erosion;getSingleErosionValue(FFF[F)F";

    @Unique
    private static final double FIRST_OCTAVE_SCALE = 1.0;

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
        try (RtfLap.Frame.Scope lattice = lap.octave(scale / distance)) {
            return original.call(erosion, lap.latticeU(blockX.get(), blockZ.get()) * distance,
                    lap.latticeV(blockZ.get()) * distance, distance, cache);
        }
    }

    @WrapOperation(method = "getSingleErosionValue", at = @At(value = "INVOKE",
            target = "Lraccoonman/reterraforged/world/worldgen/noise/module/Erosion;getNoiseValue(IIFF"
                    + "Lraccoonman/reterraforged/world/worldgen/noise/module/Noise;[F)F"))
    private float exotic_worlds$canonicalCandidate(int dx, int dy, float px, float py, Noise module, float[] cache,
            Operation<Float> original, @Local(argsOnly = true, ordinal = GRID_SIZE_ORDINAL) float cellSize) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(dx, dy, px, py, module, cache);
        }

        int cellX = NoiseUtil.floor(px / cellSize);
        int cellY = NoiseUtil.floor(py / cellSize);
        int canonicalX = frame.foldX(cellX, cellY);
        int canonicalY = frame.foldZ(cellY);
        NoiseUtil.Vec2f jitter = NoiseUtil.cell(this.seed, canonicalX, canonicalY);
        float candidateX = (canonicalX + jitter.x()) * cellSize;
        float candidateY = (canonicalY + jitter.y()) * cellSize;
        try (RtfLap.Frame.Scope open = frame.open()) {
            return original.call(dx, dy, candidateX, candidateY, module, cache);
        }
    }
}
