package com.exoticworlds.compat.naturaltemperature.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;

import com.bawnorton.mixinsquared.TargetHandler;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.naturaltemperature.ClimateLap;
import com.exoticworlds.compat.naturaltemperature.PinnedSample;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import naturaltemperature.NoiseBiomeBandsGenerator;

import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;

@Mixin(value = Climate.Sampler.class, priority = 1100)
public class WorldPainterMixin {
    private static final String WORLD_PAINTER = "naturaltemperature.mixin.WorldPainter";

    private static final String PAINT = "modifyTemperature";

    private static final String TEMPERATURE = "customTemperatureCalculation(III)F";

    private static final String HUMIDITY = "customHumidityCalculation(III)F";

    private static final String SQRT = "Ljava/lang/Math;sqrt(D)D";

    private static final String SIN = "Ljava/lang/Math;sin(D)D";

    private static final String WAVE_FREQUENCY = "waveFrequency";

    private static final String MAGNIFICATION = "magnification";

    private static final String MODULATED_FREQUENCY = "modulatedFrequency";

    private static final String MODULATED_HUMIDITY_FREQUENCY = "modulatedUFrequency";

    private static final int NOISE_OCTAVE = 0;

    private static final double NOISE_FREQUENCY = 0x1.0p-7;

    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/biome/Climate$Sampler;customTemperatureCalculation(III)F"))
    @TargetHandler(mixin = WORLD_PAINTER, name = PAINT)
    private float exotic_worlds$paintTheLapTemperature(Climate.Sampler sampler, int x, int y, int z,
            Operation<Float> original) {
        ClimateLap lap = ClimateLap.bound();
        if (lap == null) {
            return original.call(sampler, x, y, z);
        }

        PinnedSample previous = PinnedSample.enter(lap, x, y, z);
        try {
            return original.call(sampler, lap.x(x, z), y, lap.z(z));
        } finally {
            PinnedSample.exit(previous);
        }
    }

    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/biome/Climate$Sampler;customHumidityCalculation(III)F"))
    @TargetHandler(mixin = WORLD_PAINTER, name = PAINT)
    private float exotic_worlds$paintTheLapHumidity(Climate.Sampler sampler, int x, int y, int z,
            Operation<Float> original) {
        ClimateLap lap = ClimateLap.bound();
        if (lap == null) {
            return original.call(sampler, x, y, z);
        }

        PinnedSample previous = PinnedSample.enter(lap, x, y, z);
        try {
            return original.call(sampler, lap.x(x, z), y, lap.z(z));
        } finally {
            PinnedSample.exit(previous);
        }
    }

    @WrapOperation(
            method = {TEMPERATURE, HUMIDITY},
            at = @At(value = "INVOKE", target = InjectionTargets.DENSITY_FUNCTION_COMPUTE))
    private double exotic_worlds$readTheRawFieldWherePainted(DensityFunction field,
            DensityFunction.FunctionContext context, Operation<Double> original) {
        return original.call(field, PinnedSample.at(context));
    }

    @WrapOperation(
            method = {TEMPERATURE, HUMIDITY},
            at = @At(value = "INVOKE", target = SQRT),
            slice = @Slice(to = @At(value = "INVOKE", target = SIN, ordinal = 0)))
    private double exotic_worlds$turnTheLap(double value, Operation<Double> original,
            @Local(name = WAVE_FREQUENCY) double waveFrequency, @Local(name = MAGNIFICATION) double magnification,
            @Local(name = MODULATED_FREQUENCY) double modulatedFrequency) {
        double frequency = original.call(value);
        PinnedSample sample = PinnedSample.current();
        return sample == null
                ? frequency
                : sample.lap().lapFrequency(frequency, waveFrequency / magnification, modulatedFrequency);
    }

    @WrapOperation(
            method = {TEMPERATURE, HUMIDITY},
            at = @At(value = "INVOKE", target = "Lnaturaltemperature/NoiseBiomeBandsGenerator;getNoise(DDD)D"))
    private double exotic_worlds$noiseOnTheLap(NoiseBiomeBandsGenerator noise, double x, double z, double scale,
            Operation<Double> original) {
        PinnedSample sample = PinnedSample.current();
        if (sample == null || !sample.lap().closesNoise()) {
            return original.call(noise, x, z, scale);
        }

        @Nullable ImprovedNoise field = ((NoiseBiomeBandsGeneratorAccessor) noise).exotic_worlds$noise()
                .getOctaveNoise(NOISE_OCTAVE);
        if (field == null) {
            return original.call(noise, x, z, scale);
        }

        return sample.lap().noise(field.p, field.xo, field.yo, field.zo, scale * NOISE_FREQUENCY, x, z,
                sample.blockZ());
    }

    @ModifyVariable(method = {TEMPERATURE, HUMIDITY}, at = @At("STORE"), name = "wave")
    private double exotic_worlds$foldAfterTheWave(double wave, @Local(argsOnly = true, ordinal = 2) int z,
            @Local(name = "noise") double noise, @Local(name = "distortionAmplitude") double distortionAmplitude) {
        PinnedSample sample = PinnedSample.current();
        return sample == null ? wave : sample.lap().carriedWave(sample.blockZ(), z, wave, noise * distortionAmplitude);
    }

    @ModifyVariable(method = HUMIDITY, at = @At("STORE"), name = "hbw")
    private double exotic_worlds$bandsOnTheLap(double width) {
        PinnedSample sample = PinnedSample.current();
        return sample == null ? width : sample.lap().lapBandWidth(width);
    }

    @WrapOperation(method = HUMIDITY, at = @At(value = "INVOKE", target = SIN, ordinal = 4))
    private double exotic_worlds$firstZWave(double angle, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 2) int z, @Local(name = "fq1") double frequency,
            @Local(name = WAVE_FREQUENCY) double waveFrequency, @Local(name = MAGNIFICATION) double magnification,
            @Local(name = MODULATED_FREQUENCY) double modulatedFrequency,
            @Local(name = MODULATED_HUMIDITY_FREQUENCY) double modulatedHumidityFrequency) {
        return original.call(exotic_worlds$zAngle(angle, z, frequency, waveFrequency / magnification,
                modulatedFrequency, modulatedHumidityFrequency));
    }

    @WrapOperation(method = HUMIDITY, at = @At(value = "INVOKE", target = SIN, ordinal = 5))
    private double exotic_worlds$secondZWave(double angle, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 2) int z, @Local(name = "fq2") double frequency,
            @Local(name = WAVE_FREQUENCY) double waveFrequency, @Local(name = MAGNIFICATION) double magnification,
            @Local(name = MODULATED_FREQUENCY) double modulatedFrequency,
            @Local(name = MODULATED_HUMIDITY_FREQUENCY) double modulatedHumidityFrequency) {
        return original.call(exotic_worlds$zAngle(angle, z, frequency, waveFrequency / magnification,
                modulatedFrequency, modulatedHumidityFrequency));
    }

    @WrapOperation(method = HUMIDITY, at = @At(value = "INVOKE", target = SIN, ordinal = 6))
    private double exotic_worlds$thirdZWave(double angle, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 2) int z, @Local(name = "fq3") double frequency,
            @Local(name = WAVE_FREQUENCY) double waveFrequency, @Local(name = MAGNIFICATION) double magnification,
            @Local(name = MODULATED_FREQUENCY) double modulatedFrequency,
            @Local(name = MODULATED_HUMIDITY_FREQUENCY) double modulatedHumidityFrequency) {
        return original.call(exotic_worlds$zAngle(angle, z, frequency, waveFrequency / magnification,
                modulatedFrequency, modulatedHumidityFrequency));
    }

    @WrapOperation(method = HUMIDITY, at = @At(value = "INVOKE", target = SIN, ordinal = 7))
    private double exotic_worlds$fourthZWave(double angle, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 2) int z, @Local(name = "fq4") double frequency,
            @Local(name = WAVE_FREQUENCY) double waveFrequency, @Local(name = MAGNIFICATION) double magnification,
            @Local(name = MODULATED_FREQUENCY) double modulatedFrequency,
            @Local(name = MODULATED_HUMIDITY_FREQUENCY) double modulatedHumidityFrequency) {
        return original.call(exotic_worlds$zAngle(angle, z, frequency, waveFrequency / magnification,
                modulatedFrequency, modulatedHumidityFrequency));
    }

    @Unique
    private static double exotic_worlds$zAngle(double angle, int z, double frequency, double nominalRate,
            double rate, double zRate) {
        PinnedSample sample = PinnedSample.current();
        return sample == null
                ? angle
                : sample.lap().zWaveAngle(angle, frequency, nominalRate, rate, zRate, z, sample.blockZ());
    }
}
