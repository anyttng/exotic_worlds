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
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.exoticworlds.compat.terraforged.LapOctaves;
import com.exoticworlds.compat.terraforged.RtfLap;
import com.exoticworlds.compat.terraforged.SimplexOnSquare;
import com.exoticworlds.compat.ultraterraforged.UltraTerraForgedNoise;

import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Simplex;

@Mixin(value = Simplex.class, remap = false)
public abstract class SimplexMixin {
    @Unique
    private static final String SAMPLE = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/noise/module/Simplex;"
            + "sample(FFI)F";

    @Shadow
    @Final
    private float frequency;

    @Shadow
    @Final
    private float lacunarity;

    @WrapMethod(method = "compute")
    private float exotic_worlds$closeOnLap(float x, float z, int seed, Operation<Float> original,
            @Share(LapOctaves.FRAME) LocalRef<RtfLap.@Nullable Frame> frame,
            @Share(LapOctaves.BLOCK_X) LocalFloatRef blockX, @Share(LapOctaves.BLOCK_Z) LocalFloatRef blockZ,
            @Share(LapOctaves.FREQUENCY) LocalDoubleRef frequency) {
        LapOctaves.open(frame, blockX, blockZ, frequency, x, z, this.frequency * SimplexOnSquare.RATE);
        return original.call(x, z, seed);
    }

    // The simplex lattice is triangular and cannot close on a square lap, so the square stand-in answers for it.
    @WrapOperation(method = "compute", at = @At(value = "INVOKE", target = SAMPLE))
    private float exotic_worlds$octaveOnLap(float x, float z, int seed, Operation<Float> original,
            @Share(LapOctaves.FRAME) LocalRef<RtfLap.@Nullable Frame> frame,
            @Share(LapOctaves.BLOCK_X) LocalFloatRef blockX, @Share(LapOctaves.BLOCK_Z) LocalFloatRef blockZ,
            @Share(LapOctaves.FREQUENCY) LocalDoubleRef frequency) {
        RtfLap.Frame lap = frame.get();
        if (lap == null) {
            return original.call(x, z, seed);
        }

        try (RtfLap.Frame.Scope _ = LapOctaves.next(lap, frequency, this.lacunarity)) {
            return SimplexOnSquare.simplex(UltraTerraForgedNoise.INSTANCE, lap.latticeU(blockX.get(), blockZ.get()),
                    lap.latticeV(blockZ.get()), seed);
        }
    }
}
