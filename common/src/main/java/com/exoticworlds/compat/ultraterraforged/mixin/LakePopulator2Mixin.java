package com.exoticworlds.compat.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.exoticworlds.compat.terraforged.RiverReach;
import com.exoticworlds.compat.ultraterraforged.UltraTerraForgedInjectionTargets;

import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator.LakePopulator2;

@Mixin(value = LakePopulator2.class, remap = false)
public abstract class LakePopulator2Mixin {
    @Unique
    private static final String CONSTRUCTOR = "<init>(Lcom/pandaismyname1/ultraterraforged/world/worldgen/noise/"
            + "NoiseUtil$Vec2f;FFLcom/pandaismyname1/ultraterraforged/world/worldgen/rivermap/lake/LakeConfig;)V";

    @ModifyExpressionValue(method = CONSTRUCTOR,
            at = @At(value = "CONSTANT", args = UltraTerraForgedInjectionTargets.VALLEY_WIDTH))
    private float exotic_worlds$valleySizedForTheLap(float blocks) {
        return blocks * RiverReach.boundScale();
    }
}
