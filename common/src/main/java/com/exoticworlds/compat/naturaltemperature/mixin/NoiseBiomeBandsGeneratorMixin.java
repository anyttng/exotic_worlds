package com.exoticworlds.compat.naturaltemperature.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.compat.naturaltemperature.BandNoises;

import naturaltemperature.NoiseBiomeBandsGenerator;

import net.minecraft.world.level.levelgen.synth.NormalNoise;

@Mixin(NoiseBiomeBandsGenerator.class)
public class NoiseBiomeBandsGeneratorMixin implements BandNoises {
    @Shadow
    @Final
    private NormalNoise largeNoise;

    @Shadow
    @Final
    private NormalNoise detailNoise;

    @Override
    public NormalNoise exotic_worlds$large() {
        return this.largeNoise;
    }

    @Override
    public NormalNoise exotic_worlds$detail() {
        return this.detailNoise;
    }
}
