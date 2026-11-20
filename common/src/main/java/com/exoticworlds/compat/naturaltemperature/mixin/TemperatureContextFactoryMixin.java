package com.exoticworlds.compat.naturaltemperature.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.compat.naturaltemperature.BandNoises;
import com.exoticworlds.compat.naturaltemperature.NaturalTemperatureInjectionTargets;
import com.exoticworlds.compat.naturaltemperature.NaturalWaveCells;
import com.exoticworlds.compat.naturaltemperature.PinnedSample;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import naturaltemperature.NoiseBiomeBandsGenerator;
import naturaltemperature.TemperatureContextFactory;

@Mixin(TemperatureContextFactory.class)
public class TemperatureContextFactoryMixin {
    private static final String CREATE = "create";

    private static final String WAVE_FREQUENCY = "waveFrequency";

    private static final String MAGNIFICATION = "magnification";

    @WrapOperation(
            method = CREATE,
            at = @At(value = "INVOKE", target = NaturalTemperatureInjectionTargets.LARGE_NOISE))
    private static double exotic_worlds$largeNoiseOnTheLap(NoiseBiomeBandsGenerator noise, double x, double z,
            double scale, Operation<Double> original) {
        PinnedSample sample = PinnedSample.current();
        if (sample == null) {
            return original.call(noise, x, z, scale);
        }

        return sample.lap().noise(((BandNoises) noise).exotic_worlds$large(), scale, sample.signedX(), z,
                sample.blockZ());
    }

    @WrapOperation(
            method = CREATE,
            at = @At(value = "INVOKE", target = NaturalTemperatureInjectionTargets.DETAIL_NOISE))
    private static double exotic_worlds$detailNoiseOnTheLap(NoiseBiomeBandsGenerator noise, double x, double z,
            double scale, Operation<Double> original) {
        PinnedSample sample = PinnedSample.current();
        if (sample == null) {
            return original.call(noise, x, z, scale);
        }

        return sample.lap().noise(((BandNoises) noise).exotic_worlds$detail(), scale, sample.signedX(), z,
                sample.blockZ());
    }

    @WrapOperation(
            method = CREATE,
            at = @At(value = "INVOKE", target = NaturalTemperatureInjectionTargets.NATURAL_WAVE))
    private static double exotic_worlds$waveOnTheXLap(double x, double amplitude, double frequency, long seed,
            Operation<Double> original, @Local(name = WAVE_FREQUENCY) double waveFrequency,
            @Local(name = MAGNIFICATION) double magnification) {
        PinnedSample sample = PinnedSample.current();
        if (sample == null || !sample.lap().closesXWave()) {
            return original.call(x, amplitude, frequency, seed);
        }

        return sample.lap().xWave(NaturalWaveCells.INSTANCE, seed, sample.signedX(), amplitude,
                waveFrequency / magnification);
    }

    @ModifyVariable(method = CREATE, at = @At(value = "LOAD", ordinal = 0), name = "wave")
    private static double exotic_worlds$foldAfterTheWave(double wave, @Local(argsOnly = true, ordinal = 2) int z,
            @Local(name = "distorsion") double distortion) {
        PinnedSample sample = PinnedSample.current();
        return sample == null ? wave : sample.lap().carriedWave(sample.blockZ(), z, wave, distortion);
    }
}
