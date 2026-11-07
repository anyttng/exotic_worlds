package com.exoticworlds.compat.reterraforged.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.reterraforged.ReTerraForgedRiverReach;
import com.exoticworlds.compat.terraforged.RiverReach;
import com.exoticworlds.compat.terraforged.RtfLap;

import raccoonman.reterraforged.world.worldgen.cell.continent.SimpleContinent;
import raccoonman.reterraforged.world.worldgen.cell.continent.simple.SimpleRiverGenerator;

@Mixin(value = SimpleRiverGenerator.class, remap = false)
public abstract class SimpleRiverGeneratorMixin {
    @Unique
    private static final String DISTANCE_TO_OCEAN =
            "Lraccoonman/reterraforged/world/worldgen/cell/continent/SimpleContinent;getDistanceToOcean(IIFF)F";

    @Unique
    private static final String COUNT =
            "Lraccoonman/reterraforged/world/worldgen/cell/continent/simple/SimpleRiverGenerator;count:I";

    @Unique
    private static final String ROOT_START = "floatValue=400.0F";

    @ModifyExpressionValue(method = "generateRoots",
            at = @At(value = "FIELD", target = COUNT, opcode = Opcodes.GETFIELD))
    private int exotic_worlds$rootsForTheCell(int count) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return count;
        }

        return RiverReach.rootCount(frame, count,
                ((BaseRiverGeneratorAccessor) (Object) this).exotic_worlds$continentScale());
    }

    @ModifyExpressionValue(method = "generateRoots", at = @At(value = "CONSTANT", args = ROOT_START))
    private float exotic_worlds$sizedForTheLap(float blocks) {
        return blocks * ReTerraForgedRiverReach.INSTANCE.boundScale();
    }

    @WrapOperation(method = "generateRoots", at = @At(value = "INVOKE", target = DISTANCE_TO_OCEAN))
    private float exotic_worlds$fitInsideHalfLap(SimpleContinent continent, int x, int z, float dx, float dz,
            Operation<Float> original) {
        float length = original.call(continent, x, z, dx, dz);
        RtfLap.Frame frame = RtfLap.boundFrame();
        return frame == null ? length : ReTerraForgedRiverReach.INSTANCE.clamp(frame, length);
    }
}
