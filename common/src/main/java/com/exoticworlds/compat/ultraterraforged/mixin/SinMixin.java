package com.exoticworlds.compat.ultraterraforged.mixin;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.exoticworlds.compat.terraforged.LapOctaves;
import com.exoticworlds.compat.terraforged.RtfLap;

import net.minecraft.core.Direction;

@Mixin(targets = "com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Sin", remap = false)
public abstract class SinMixin {
    @Unique
    private static final String FREQUENCY =
            "Lcom/pandaismyname1/ultraterraforged/world/worldgen/noise/module/Sin;frequency:F";

    @Unique
    private static final String SIN = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/noise/NoiseUtil;sin(F)F";

    @Unique
    private static final String SHEAR = "lapShear";

    @Unique
    private static final int X_READ = 0;

    @Unique
    private static final int Z_READ = 1;

    @Unique
    private static final int X_SIN = 0;

    @Unique
    private static final int BLENDED_X_SIN = 2;

    @WrapMethod(method = "compute")
    private float exotic_worlds$closeOnLap(float x, float z, int seed, Operation<Float> original,
            @Share(LapOctaves.FRAME) LocalRef<RtfLap.@Nullable Frame> frame,
            @Share(LapOctaves.BLOCK_Z) LocalFloatRef blockZ) {
        RtfLap.Frame bound = RtfLap.boundFrame();
        frame.set(bound);
        blockZ.set(z);
        return original.call(x, z, seed);
    }

    @ModifyExpressionValue(method = "compute",
            at = @At(value = "FIELD", target = FREQUENCY, opcode = Opcodes.GETFIELD, ordinal = X_READ))
    private float exotic_worlds$turnsAlongX(float frequency,
            @Share(LapOctaves.FRAME) LocalRef<RtfLap.@Nullable Frame> frame, @Share(SHEAR) LocalFloatRef shear) {
        RtfLap.Frame lap = frame.get();
        if (lap == null) {
            return frequency;
        }

        float angularX = lap.snappedAngular(Direction.Axis.X, frequency);
        shear.set(lap.angularShear(angularX));
        return angularX;
    }

    @ModifyExpressionValue(method = "compute",
            at = @At(value = "FIELD", target = FREQUENCY, opcode = Opcodes.GETFIELD, ordinal = Z_READ))
    private float exotic_worlds$turnsAlongZ(float frequency,
            @Share(LapOctaves.FRAME) LocalRef<RtfLap.@Nullable Frame> frame) {
        RtfLap.Frame lap = frame.get();
        return lap == null ? frequency : lap.snappedAngular(Direction.Axis.Z, frequency);
    }

    @WrapOperation(method = "compute", at = {@At(value = "INVOKE", target = SIN, ordinal = X_SIN),
            @At(value = "INVOKE", target = SIN, ordinal = BLENDED_X_SIN)})
    private float exotic_worlds$shearAlongX(float phase, Operation<Float> original,
            @Share(LapOctaves.BLOCK_Z) LocalFloatRef blockZ, @Share(SHEAR) LocalFloatRef shear) {
        float lapShear = shear.get();
        return original.call(lapShear == RtfLap.UNSHEARED ? phase : phase + blockZ.get() * lapShear);
    }
}
