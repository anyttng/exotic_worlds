package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.reterraforged.RtfLap;

import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.function.DistanceFunction;
import raccoonman.reterraforged.world.worldgen.noise.function.EdgeFunction;

@Mixin(targets = "raccoonman.reterraforged.world.worldgen.noise.module.WorleyEdge", remap = false)
public abstract class WorleyEdgeMixin {
    @Shadow
    @Final
    private float frequency;

    @Shadow
    @Final
    private float distance;

    @Shadow
    @Final
    private EdgeFunction edgeFunction;

    @Shadow
    @Final
    private DistanceFunction distanceFunction;

    @Shadow
    public static float sample(float x, float y, int seed, float distance, EdgeFunction edgeFunction,
            DistanceFunction distanceFunc) {
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
            value = sample(latticeX, latticeZ, seed, this.distance, this.edgeFunction, this.distanceFunction);
        }

        return NoiseUtil.map(value, this.edgeFunction.min(), this.edgeFunction.max(), this.edgeFunction.range());
    }
}
