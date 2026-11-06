package com.exoticworlds.compat.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.ultraterraforged.UltraTerraForgedRiverReach;

import com.pandaismyname1.ultraterraforged.world.worldgen.noise.domain.Domain;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.gen.GenWarp;

@Mixin(value = GenWarp.class, remap = false)
public abstract class GenWarpMixin {
    @Unique
    private static final String DOMAIN_PERLIN = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/noise/domain/"
            + "Domains;domainPerlin(IIIF)Lcom/pandaismyname1/ultraterraforged/world/worldgen/noise/domain/Domain;";

    @Unique
    private static final int WARPS = 4;

    @Unique
    private static final int MIN_WAVELENGTH = 1;

    @WrapOperation(method = "make", at = @At(value = "INVOKE", target = DOMAIN_PERLIN), require = WARPS)
    private static Domain exotic_worlds$warpForTheLap(int seed, int wavelength, int octaves, float strength,
            Operation<Domain> original) {
        float lapScale = UltraTerraForgedRiverReach.INSTANCE.boundScale();
        return original.call(seed, Math.max(MIN_WAVELENGTH, Math.round(wavelength * lapScale)), octaves,
                strength * lapScale);
    }
}
