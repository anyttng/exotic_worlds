package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.reterraforged.RtfLap;

import net.minecraft.core.Direction;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Erosion;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;

@Mixin(value = Erosion.class, remap = false)
public abstract class ErosionNoiseMixin {
    private static final int GRID_SIZE_ORDINAL = 2;

    @Shadow
    @Final
    private int seed;

    @Shadow
    @Final
    private int octaves;

    @Shadow
    @Final
    private float gridSize;

    @Shadow
    @Final
    private float amplitude;

    @Shadow
    @Final
    private float lacunarity;

    @Shadow
    @Final
    private float distanceFallOff;

    @Shadow
    private float getSingleErosionValue(float x, float y, float gridSize, float[] cache) {
        throw new AssertionError();
    }

    @WrapMethod(method = "getErosionValue")
    private float toroidal$closeOnLap(float x, float y, float[] cache, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(x, y, cache);
        }

        float shiftedX = frame.shift(Direction.Axis.X, x);
        float shiftedY = frame.shift(Direction.Axis.Z, y);
        float sum = 0.0F;
        float max = 0.0F;
        float gain = 1.0F;
        float distance = this.gridSize;
        double octaveScale = 1.0;
        for (int octave = 0; octave < this.octaves; octave++) {
            float value;
            try (RtfLap.Frame.Scope lattice = frame.octave(octaveScale / distance)) {
                float latticeX = shiftedX * frame.xScale() * distance;
                float latticeY = shiftedY * frame.zScale() * distance;
                value = getSingleErosionValue(latticeX, latticeY, distance, cache);
            }

            sum += value * gain;
            max += gain;
            gain *= this.amplitude;
            distance *= this.distanceFallOff;
            octaveScale *= this.lacunarity;
        }

        return sum / max;
    }

    // A candidate point is one lattice cell's jittered point, so its height is read where the cell's canonical index
    // puts it; the jitter keeps it well inside the cell, so the index is recovered by flooring.
    @WrapOperation(method = "getSingleErosionValue", at = @At(value = "INVOKE",
            target = "Lraccoonman/reterraforged/world/worldgen/noise/module/Erosion;getNoiseValue(IIFF"
                    + "Lraccoonman/reterraforged/world/worldgen/noise/module/Noise;[F)F"))
    private float toroidal$canonicalCandidate(int dx, int dy, float px, float py, Noise module, float[] cache,
            Operation<Float> original, @Local(argsOnly = true, ordinal = GRID_SIZE_ORDINAL) float cellSize) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(dx, dy, px, py, module, cache);
        }

        int cellX = frame.fold(Direction.Axis.X, NoiseUtil.floor(px / cellSize));
        int cellY = frame.fold(Direction.Axis.Z, NoiseUtil.floor(py / cellSize));
        NoiseUtil.Vec2f vector = NoiseUtil.cell(this.seed, cellX, cellY);
        try (RtfLap.Frame.Scope open = frame.open()) {
            return original.call(dx, dy, (cellX + vector.x()) * cellSize, (cellY + vector.y()) * cellSize, module,
                    cache);
        }
    }
}
