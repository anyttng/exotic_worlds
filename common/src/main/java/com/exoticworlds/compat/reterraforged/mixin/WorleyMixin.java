package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.reterraforged.RtfLap;

import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.function.CellFunction;
import raccoonman.reterraforged.world.worldgen.noise.function.DistanceFunction;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;

@Mixin(targets = "raccoonman.reterraforged.world.worldgen.noise.module.Worley", remap = false)
public abstract class WorleyMixin {
    @Shadow
    @Final
    private float frequency;

    @Shadow
    @Final
    private float distance;

    @Shadow
    @Final
    private CellFunction cellFunction;

    @Shadow
    @Final
    private DistanceFunction distanceFunction;

    @Shadow
    @Final
    private Noise lookup;

    @Shadow
    @Final
    private float min;

    @Shadow
    @Final
    private float max;

    @Shadow
    public static float sample(float x, float y, int seed, float distance, CellFunction cellFunction,
            DistanceFunction distanceFunction, Noise lookup) {
        throw new AssertionError();
    }

    @WrapMethod(method = "compute")
    private float toroidal$closeOnLap(float x, float z, int seed, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(x, z, seed);
        }

        float value;
        try (RtfLap.Frame.Scope lattice = frame.octave(this.frequency)) {
            float latticeX = frame.latticeX(x, z) * frame.xScale();
            float latticeZ = frame.shiftZ(z) * frame.zScale();
            value = sample(latticeX, latticeZ, seed, this.distance, this.cellFunction, this.distanceFunction,
                    this.lookup);
        }

        return this.cellFunction.mapValue(value, this.min, this.max, this.max - this.min);
    }

    // A copy one lap away spells the winning cell P cells further on, so its value is read at the folded index.
    @WrapOperation(method = "sample", at = @At(value = "INVOKE",
            target = "Lraccoonman/reterraforged/world/worldgen/noise/function/CellFunction;apply(IIIF"
            + "Lraccoonman/reterraforged/world/worldgen/noise/NoiseUtil$Vec2f;"
            + "Lraccoonman/reterraforged/world/worldgen/noise/module/Noise;)F"))
    private static float toroidal$canonicalCell(CellFunction function, int seed, int cellX, int cellY, float nearest,
            NoiseUtil.Vec2f vector, Noise lookup, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(function, seed, cellX, cellY, nearest, vector, lookup);
        }

        int canonicalX = frame.foldX(cellX, cellY);
        int canonicalY = frame.foldZ(cellY);
        try (RtfLap.Frame.Scope open = frame.open()) {
            return original.call(function, seed, canonicalX, canonicalY, nearest, vector, lookup);
        }
    }
}
