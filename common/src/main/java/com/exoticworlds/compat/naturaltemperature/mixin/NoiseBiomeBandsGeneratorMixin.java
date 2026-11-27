package com.exoticworlds.compat.naturaltemperature.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.naturaltemperature.BandNoises;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.naturaltemperature.NoiseBiomeBandsGenerator;

import net.minecraft.world.level.levelgen.synth.Noise;
import net.minecraft.world.level.levelgen.synth.NoiseStack;

@Mixin(NoiseBiomeBandsGenerator.class)
public class NoiseBiomeBandsGeneratorMixin implements BandNoises {
    private static final String CONSTRUCTORS = "<init>*";

    private static final String CREATE = "Lnet/minecraft/world/level/levelgen/synth/NormalNoise;create("
            + "Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/level/levelgen/synth/Noise;";

    @Unique
    private NoiseStack exotic_worlds$large;

    @Unique
    private NoiseStack exotic_worlds$detail;

    @ModifyExpressionValue(method = CONSTRUCTORS, at = @At(value = "INVOKE", target = CREATE, ordinal = 0))
    private Noise exotic_worlds$keepLarge(Noise noise) {
        this.exotic_worlds$large = (NoiseStack) noise;
        return noise;
    }

    @ModifyExpressionValue(method = CONSTRUCTORS, at = @At(value = "INVOKE", target = CREATE, ordinal = 1))
    private Noise exotic_worlds$keepDetail(Noise noise) {
        this.exotic_worlds$detail = (NoiseStack) noise;
        return noise;
    }

    @Override
    public NoiseStack exotic_worlds$getLarge() {
        return this.exotic_worlds$large;
    }

    @Override
    public NoiseStack exotic_worlds$getDetail() {
        return this.exotic_worlds$detail;
    }
}
