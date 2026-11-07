package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.reterraforged.ReTerraForgedRiverReach;

import raccoonman.reterraforged.world.worldgen.cell.rivermap.gen.GenWarp;
import raccoonman.reterraforged.world.worldgen.noise.domain.Domain;

@Mixin(value = GenWarp.class, remap = false)
public abstract class GenWarpMixin {
    @Unique
    private static final String DOMAIN_PERLIN = "Lraccoonman/reterraforged/world/worldgen/noise/domain/"
            + "Domains;domainPerlin(IIIF)Lraccoonman/reterraforged/world/worldgen/noise/domain/Domain;";

    @Unique
    private static final int WARPS = 4;

    @Unique
    private static final int MIN_WAVELENGTH = 1;

    @WrapOperation(method = "make", at = @At(value = "INVOKE", target = DOMAIN_PERLIN), require = WARPS)
    private static Domain exotic_worlds$warpForTheLap(int seed, int wavelength, int octaves, float strength,
            Operation<Domain> original) {
        float lapScale = ReTerraForgedRiverReach.INSTANCE.boundScale();
        return original.call(seed, Math.max(MIN_WAVELENGTH, Math.round(wavelength * lapScale)), octaves,
                strength * lapScale);
    }
}
