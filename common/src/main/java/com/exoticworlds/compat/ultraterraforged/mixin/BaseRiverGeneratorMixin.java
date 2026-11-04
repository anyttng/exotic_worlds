package com.exoticworlds.compat.ultraterraforged.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.exoticworlds.compat.terraforged.RiverReach;
import com.exoticworlds.compat.ultraterraforged.UltraTerraForgedInjectionTargets;

import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.river.BaseRiverGenerator;

@Mixin(value = BaseRiverGenerator.class, remap = false)
public abstract class BaseRiverGeneratorMixin {
    @Unique
    private static final String FORK_MIN_LENGTH = "floatValue=300.0F";

    @Unique
    private static final String OVERLAP_DISTANCE = "floatValue=250.0F";

    @Unique
    private static final String LAKE_SIZE_MIN =
            "Lcom/pandaismyname1/ultraterraforged/world/worldgen/rivermap/lake/LakeConfig;sizeMin:F";

    @Unique
    private static final String LAKE_SIZE_RANGE =
            "Lcom/pandaismyname1/ultraterraforged/world/worldgen/rivermap/lake/LakeConfig;sizeRange:F";

    @Unique
    private static final String VARIANCE_NEXT =
            "Lcom/pandaismyname1/ultraterraforged/world/worldgen/util/Variance;next(Ljava/util/Random;)F";

    @ModifyExpressionValue(method = "generateForks", at = {
            @At(value = "CONSTANT", args = FORK_MIN_LENGTH),
            @At(value = "CONSTANT", args = UltraTerraForgedInjectionTargets.VALLEY_WIDTH)}, require = 2)
    private float exotic_worlds$forkSizedForTheLap(float blocks) {
        return blocks * RiverReach.boundScale();
    }

    @ModifyExpressionValue(method = "riverOverlaps", at = @At(value = "CONSTANT", args = OVERLAP_DISTANCE))
    private float exotic_worlds$overlapSizedForTheLap(float blocks) {
        return blocks * RiverReach.boundScale();
    }

    @ModifyExpressionValue(method = "addLake", at = {
            @At(value = "FIELD", target = LAKE_SIZE_MIN, opcode = Opcodes.GETFIELD),
            @At(value = "FIELD", target = LAKE_SIZE_RANGE, opcode = Opcodes.GETFIELD)}, require = 2)
    private float exotic_worlds$lakeSizedForTheLap(float blocks) {
        return blocks * RiverReach.boundScale();
    }

    @ModifyExpressionValue(method = "generateWetlands", at = @At(value = "INVOKE", target = VARIANCE_NEXT),
            require = 2)
    private float exotic_worlds$wetlandSizedForTheLap(float blocks) {
        return blocks * RiverReach.boundScale();
    }
}
