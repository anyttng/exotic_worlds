package com.exoticworlds.compat.ultraterraforged.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.ultraterraforged.UltraTerraForgedInjectionTargets;
import com.exoticworlds.compat.terraforged.RtfClimateScales;
import com.exoticworlds.compat.terraforged.RtfLap;
import com.exoticworlds.engine.noise.ClimateScaleCompression;

import net.minecraft.core.Direction;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.climate.BiomeNoise;
import com.pandaismyname1.ultraterraforged.world.worldgen.continent.Continent;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;

@Mixin(value = BiomeNoise.class, remap = false)
public abstract class BiomeNoiseMixin implements RtfClimateScales {
    @Unique
    private static final String APPLY = "apply(Lcom/pandaismyname1/ultraterraforged/world/worldgen/cell/Cell;FFFFZ)V";

    @Unique
    private static final String BIOME_FREQUENCY =
            "Lcom/pandaismyname1/ultraterraforged/world/worldgen/climate/BiomeNoise;biomeFreq:F";

    @Unique
    private static final int X_READ = 0;

    @Unique
    private static final int Z_READ = 1;

    @Unique
    private static final int CENTER_X_READ = 2;

    @Unique
    private static final int CENTER_Z_READ = 3;

    @Unique
    private static final int MOISTURE_SCALE_LOCAL = 1;

    @Unique
    private static final int TEMPERATURE_SCALE_LOCAL = 2;

    @Unique
    private int exotic_worlds$temperatureScale;

    @Unique
    private int exotic_worlds$moistureScale;

    @Shadow
    private float biomeFreq;

    @Shadow
    @Final
    private Noise warpX;

    @Shadow
    @Final
    private Noise warpZ;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void exotic_worlds$keepScales(CallbackInfo callback,
            @Local(ordinal = MOISTURE_SCALE_LOCAL) int moistScale,
            @Local(ordinal = TEMPERATURE_SCALE_LOCAL) int tempScale) {
        this.exotic_worlds$moistureScale = moistScale;
        this.exotic_worlds$temperatureScale = tempScale;
    }

    @Override
    public float exotic_worlds$biomeFrequency() {
        return this.biomeFreq;
    }

    @Override
    public int exotic_worlds$temperatureScale() {
        return this.exotic_worlds$temperatureScale;
    }

    @Override
    public int exotic_worlds$moistureScale() {
        return this.exotic_worlds$moistureScale;
    }

    @WrapMethod(method = APPLY)
    private void exotic_worlds$closeApply(Cell cell, float x, float z, float originalX, float originalZ, boolean mask,
            Operation<Void> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            original.call(cell, x, z, originalX, originalZ, mask);
            return;
        }

        try (RtfLap.Frame.Scope _ = frame.octave(this.biomeFreq)) {
            original.call(cell, frame.latticeX(x, z), frame.shiftZ(z), originalX,
                    originalZ, mask);
        }
    }

    @ModifyExpressionValue(method = APPLY,
            at = @At(value = "FIELD", target = BIOME_FREQUENCY, opcode = Opcodes.GETFIELD, ordinal = X_READ))
    private float exotic_worlds$snapX(float frequency) {
        return RtfLap.snapped(Direction.Axis.X, frequency);
    }

    @ModifyExpressionValue(method = APPLY,
            at = @At(value = "FIELD", target = BIOME_FREQUENCY, opcode = Opcodes.GETFIELD, ordinal = Z_READ))
    private float exotic_worlds$snapZ(float frequency) {
        return RtfLap.snapped(Direction.Axis.Z, frequency);
    }

    // A cell center divided by this read is the block the continent is asked about, which Compact biomes never moves.
    @ModifyExpressionValue(method = APPLY,
            at = @At(value = "FIELD", target = BIOME_FREQUENCY, opcode = Opcodes.GETFIELD, ordinal = CENTER_X_READ))
    private float exotic_worlds$centerBlockX(float frequency) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        return frame == null
                ? frequency
                : (float) (frame.snappedFrequency(Direction.Axis.X, frequency) * frame.compression());
    }

    @ModifyExpressionValue(method = APPLY,
            at = @At(value = "FIELD", target = BIOME_FREQUENCY, opcode = Opcodes.GETFIELD, ordinal = CENTER_Z_READ))
    private float exotic_worlds$centerBlockZ(float frequency) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        return frame == null
                ? frequency
                : (float) (frame.snappedFrequency(Direction.Axis.Z, frequency) * frame.compression());
    }

    @WrapOperation(method = APPLY, at = @At(value = "INVOKE",
            target = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/continent/Continent;getLandValue(FF)F"))
    private float exotic_worlds$landOnTheWorldLap(Continent continent, float x, float z, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null || frame.compression() == ClimateScaleCompression.NO_COMPRESSION) {
            return original.call(continent, x, z);
        }

        try (RtfLap.Frame.Scope _ = frame.expand()) {
            return original.call(continent, x, z);
        }
    }

    @WrapOperation(method = APPLY, at = @At(value = "INVOKE", target = UltraTerraForgedInjectionTargets.NOISE_COMPUTE))
    private float exotic_worlds$latticeLap(Noise noise, float x, float z, int seed, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null || noise == this.warpX || noise == this.warpZ) {
            return original.call(noise, x, z, seed);
        }

        try (RtfLap.Frame.Scope _ = frame.scale(frame.snappedFrequency(Direction.Axis.X, this.biomeFreq),
                frame.snappedFrequency(Direction.Axis.Z, this.biomeFreq))) {
            return original.call(noise, x, z, seed);
        }
    }
}
