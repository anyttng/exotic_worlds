package com.exoticworlds.compat.ultraterraforged.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.terraforged.RiverReach;
import com.exoticworlds.compat.terraforged.RtfLap;
import com.exoticworlds.compat.ultraterraforged.UltraTerraForgedInjectionTargets;

import com.pandaismyname1.ultraterraforged.world.worldgen.continent.SimpleContinent;
import com.pandaismyname1.ultraterraforged.world.worldgen.continent.simple.SimpleRiverGenerator;

@Mixin(value = SimpleRiverGenerator.class, remap = false)
public abstract class SimpleRiverGeneratorMixin {
    @Unique
    private static final String DISTANCE_TO_OCEAN =
            "Lcom/pandaismyname1/ultraterraforged/world/worldgen/continent/SimpleContinent;getDistanceToOcean(IIFF)F";

    @Unique
    private static final String COUNT =
            "Lcom/pandaismyname1/ultraterraforged/world/worldgen/continent/simple/SimpleRiverGenerator;count:I";

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

    @ModifyExpressionValue(method = "generateRoots", at = {
            @At(value = "CONSTANT", args = ROOT_START),
            @At(value = "CONSTANT", args = UltraTerraForgedInjectionTargets.VALLEY_WIDTH)}, require = 2)
    private float exotic_worlds$sizedForTheLap(float blocks) {
        return blocks * RiverReach.boundScale();
    }

    @WrapOperation(method = "generateRoots", at = @At(value = "INVOKE", target = DISTANCE_TO_OCEAN))
    private float exotic_worlds$fitInsideHalfLap(SimpleContinent continent, int x, int z, float dx, float dz,
            Operation<Float> original) {
        float length = original.call(continent, x, z, dx, dz);
        RtfLap.Frame frame = RtfLap.boundFrame();
        return frame == null ? length : RiverReach.clamp(frame, length);
    }
}
