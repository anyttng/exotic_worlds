package com.exoticworlds.compat.naturaltemperature.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;

import com.exoticworlds.compat.naturaltemperature.PinnedSample;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.naturaltemperature.NoiseBiomeBandsGenerator;
import com.naturaltemperature.Painter.PainterCore;

import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

@Mixin(PainterCore.class)
public class PainterCoreMixin {
    private static final String TEMPERATURE = "calculateTemperature";

    private static final String HUMIDITY = "calculateHumidity";

    private static final String SQRT = "Ljava/lang/Math;sqrt(D)D";

    private static final String SIN = "Ljava/lang/Math;sin(D)D";

    private static final String WAVE_FREQUENCY = "waveFrequency";

    private static final String MAGNIFICATION = "magnification";

    private static final String MODULATED_FREQUENCY = "modulatedFrequency";

    private static final String MODULATED_HUMIDITY_FREQUENCY = "modulatedUFrequency";

    @WrapOperation(
            method = {TEMPERATURE, HUMIDITY},
            at = @At(value = "INVOKE", target = SQRT),
            slice = @Slice(to = @At(value = "INVOKE", target = SIN, ordinal = 0)))
    private static double exotic_worlds$turnTheLap(double value, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 0) DensitySampler.Bound rawTemperature,
            @Local(name = WAVE_FREQUENCY) double waveFrequency, @Local(name = MAGNIFICATION) double magnification,
            @Local(name = MODULATED_FREQUENCY) double modulatedFrequency) {
        double frequency = original.call(value);
        PinnedSample sample = PinnedSample.of(rawTemperature);
        return sample == null
                ? frequency
                : sample.lap().lapFrequency(frequency, waveFrequency / magnification, modulatedFrequency);
    }

    @WrapOperation(
            method = {TEMPERATURE, HUMIDITY},
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/naturaltemperature/NoiseBiomeBandsGenerator;getNoise(DDD)D"))
    private static double exotic_worlds$noiseOnTheLap(NoiseBiomeBandsGenerator noise, double x, double z,
            double scale, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 0) DensitySampler.Bound rawTemperature) {
        PinnedSample sample = PinnedSample.of(rawTemperature);
        if (sample == null || !sample.lap().closesNoise()) {
            return original.call(noise, x, z, scale);
        }

        PerlinNoise field = ((NoiseBiomeBandsGeneratorAccessor) noise).exotic_worlds$noise();
        return sample.lap().noise(field.perms, field.offsetX, field.offsetY, field.offsetZ,
                scale * NoiseBiomeBandsGeneratorAccessor.exotic_worlds$frequency(), x, z, sample.blockZ());
    }

    @ModifyVariable(method = {TEMPERATURE, HUMIDITY}, at = @At("STORE"), name = "wave")
    private static double exotic_worlds$foldAfterTheWave(double wave,
            @Local(argsOnly = true, ordinal = 0) DensitySampler.Bound rawTemperature,
            @Local(argsOnly = true, ordinal = 2) int z, @Local(name = "noise") double noise,
            @Local(name = "distortionAmplitude") double distortionAmplitude) {
        PinnedSample sample = PinnedSample.of(rawTemperature);
        return sample == null ? wave : sample.lap().carriedWave(sample.blockZ(), z, wave, noise * distortionAmplitude);
    }

    @ModifyVariable(method = HUMIDITY, at = @At("STORE"), name = "hbw")
    private static double exotic_worlds$bandsOnTheLap(double width,
            @Local(argsOnly = true, ordinal = 0) DensitySampler.Bound rawTemperature) {
        PinnedSample sample = PinnedSample.of(rawTemperature);
        return sample == null ? width : sample.lap().lapBandWidth(width);
    }

    @WrapOperation(method = HUMIDITY, at = @At(value = "INVOKE", target = SIN, ordinal = 4))
    private static double exotic_worlds$firstZWave(double angle, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 0) DensitySampler.Bound rawTemperature,
            @Local(argsOnly = true, ordinal = 2) int z, @Local(name = "fq1") double frequency,
            @Local(name = WAVE_FREQUENCY) double waveFrequency, @Local(name = MAGNIFICATION) double magnification,
            @Local(name = MODULATED_FREQUENCY) double modulatedFrequency,
            @Local(name = MODULATED_HUMIDITY_FREQUENCY) double modulatedHumidityFrequency) {
        return original.call(exotic_worlds$zAngle(angle, rawTemperature, z, frequency, waveFrequency / magnification,
                modulatedFrequency, modulatedHumidityFrequency));
    }

    @WrapOperation(method = HUMIDITY, at = @At(value = "INVOKE", target = SIN, ordinal = 5))
    private static double exotic_worlds$secondZWave(double angle, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 0) DensitySampler.Bound rawTemperature,
            @Local(argsOnly = true, ordinal = 2) int z, @Local(name = "fq2") double frequency,
            @Local(name = WAVE_FREQUENCY) double waveFrequency, @Local(name = MAGNIFICATION) double magnification,
            @Local(name = MODULATED_FREQUENCY) double modulatedFrequency,
            @Local(name = MODULATED_HUMIDITY_FREQUENCY) double modulatedHumidityFrequency) {
        return original.call(exotic_worlds$zAngle(angle, rawTemperature, z, frequency, waveFrequency / magnification,
                modulatedFrequency, modulatedHumidityFrequency));
    }

    @WrapOperation(method = HUMIDITY, at = @At(value = "INVOKE", target = SIN, ordinal = 6))
    private static double exotic_worlds$thirdZWave(double angle, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 0) DensitySampler.Bound rawTemperature,
            @Local(argsOnly = true, ordinal = 2) int z, @Local(name = "fq3") double frequency,
            @Local(name = WAVE_FREQUENCY) double waveFrequency, @Local(name = MAGNIFICATION) double magnification,
            @Local(name = MODULATED_FREQUENCY) double modulatedFrequency,
            @Local(name = MODULATED_HUMIDITY_FREQUENCY) double modulatedHumidityFrequency) {
        return original.call(exotic_worlds$zAngle(angle, rawTemperature, z, frequency, waveFrequency / magnification,
                modulatedFrequency, modulatedHumidityFrequency));
    }

    @WrapOperation(method = HUMIDITY, at = @At(value = "INVOKE", target = SIN, ordinal = 7))
    private static double exotic_worlds$fourthZWave(double angle, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 0) DensitySampler.Bound rawTemperature,
            @Local(argsOnly = true, ordinal = 2) int z, @Local(name = "fq4") double frequency,
            @Local(name = WAVE_FREQUENCY) double waveFrequency, @Local(name = MAGNIFICATION) double magnification,
            @Local(name = MODULATED_FREQUENCY) double modulatedFrequency,
            @Local(name = MODULATED_HUMIDITY_FREQUENCY) double modulatedHumidityFrequency) {
        return original.call(exotic_worlds$zAngle(angle, rawTemperature, z, frequency, waveFrequency / magnification,
                modulatedFrequency, modulatedHumidityFrequency));
    }

    @Unique
    private static double exotic_worlds$zAngle(double angle, DensitySampler.Bound rawTemperature, int z,
            double frequency, double nominalRate, double rate, double zRate) {
        PinnedSample sample = PinnedSample.of(rawTemperature);
        return sample == null
                ? angle
                : sample.lap().zWaveAngle(angle, frequency, nominalRate, rate, zRate, z, sample.blockZ());
    }
}
