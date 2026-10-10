package com.exoticworlds.compat.reterraforged.mixin;

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
import com.exoticworlds.compat.reterraforged.ReTerraForgedInjectionTargets;
import com.exoticworlds.compat.reterraforged.RtfClimateScales;
import com.exoticworlds.compat.reterraforged.RtfLap;
import com.exoticworlds.engine.noise.ClimateScaleCompression;

import net.minecraft.core.Direction;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.climate.ClimateModule;
import raccoonman.reterraforged.world.worldgen.cell.continent.Continent;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;

@Mixin(value = ClimateModule.class, remap = false)
public abstract class ClimateModuleMixin implements RtfClimateScales {
    private static final String APPLY = "apply(Lraccoonman/reterraforged/world/worldgen/cell/Cell;FFFFZ)V";

    private static final String BIOME_FREQUENCY =
            "Lraccoonman/reterraforged/world/worldgen/cell/climate/ClimateModule;biomeFreq:F";

    private static final int X_READ = 0;

    private static final int Z_READ = 1;

    private static final int CENTER_X_READ = 2;

    private static final int CENTER_Z_READ = 3;

    private static final int MOUNTAIN_X_READ = 4;

    private static final int MOUNTAIN_Z_READ = 5;

    private static final int MOISTURE_SCALE_LOCAL = 1;

    private static final int TEMPERATURE_SCALE_LOCAL = 2;

    @Unique
    private int toroidal$temperatureScale;

    @Unique
    private int toroidal$moistureScale;

    @Shadow
    private float biomeFreq;

    @Shadow
    @Final
    private Noise warpX;

    @Shadow
    @Final
    private Noise warpZ;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void toroidal$keepScales(CallbackInfo callback,
            @Local(ordinal = MOISTURE_SCALE_LOCAL) int moistScale,
            @Local(ordinal = TEMPERATURE_SCALE_LOCAL) int tempScale) {
        this.toroidal$moistureScale = moistScale;
        this.toroidal$temperatureScale = tempScale;
    }

    @Override
    public float toroidal$biomeFrequency() {
        return this.biomeFreq;
    }

    @Override
    public int toroidal$temperatureScale() {
        return this.toroidal$temperatureScale;
    }

    @Override
    public int toroidal$moistureScale() {
        return this.toroidal$moistureScale;
    }

    @WrapMethod(method = APPLY)
    private void toroidal$closeApply(Cell cell, float x, float z, float originalX, float originalZ, boolean mask,
            Operation<Void> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            original.call(cell, x, z, originalX, originalZ, mask);
            return;
        }

        try (RtfLap.Frame.Scope lattice = frame.octave(this.biomeFreq)) {
            original.call(cell, frame.latticeX(x, z), frame.shiftZ(z), originalX,
                    originalZ, mask);
        }
    }

    @ModifyExpressionValue(method = APPLY,
            at = @At(value = "FIELD", target = BIOME_FREQUENCY, opcode = Opcodes.GETFIELD, ordinal = X_READ))
    private float toroidal$snapX(float frequency) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        return frame == null ? frequency : frame.snappedFrequency(Direction.Axis.X, frequency);
    }

    @ModifyExpressionValue(method = APPLY,
            at = @At(value = "FIELD", target = BIOME_FREQUENCY, opcode = Opcodes.GETFIELD, ordinal = Z_READ))
    private float toroidal$snapZ(float frequency) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        return frame == null ? frequency : frame.snappedFrequency(Direction.Axis.Z, frequency);
    }

    // A cell centre divided by this read is the block the continent is asked about, which Compact biomes never moves.
    @ModifyExpressionValue(method = APPLY,
            at = @At(value = "FIELD", target = BIOME_FREQUENCY, opcode = Opcodes.GETFIELD, ordinal = CENTER_X_READ))
    private float toroidal$centreBlockX(float frequency) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        return frame == null
                ? frequency
                : (float) (frame.snappedFrequency(Direction.Axis.X, frequency) * frame.compression());
    }

    @ModifyExpressionValue(method = APPLY,
            at = @At(value = "FIELD", target = BIOME_FREQUENCY, opcode = Opcodes.GETFIELD, ordinal = CENTER_Z_READ))
    private float toroidal$centreBlockZ(float frequency) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        return frame == null
                ? frequency
                : (float) (frame.snappedFrequency(Direction.Axis.Z, frequency) * frame.compression());
    }

    // A terrain region centre is a block of the world, read into the compressed climate cells here.
    @ModifyExpressionValue(method = APPLY, at = {
            @At(value = "FIELD", target = BIOME_FREQUENCY, opcode = Opcodes.GETFIELD, ordinal = MOUNTAIN_X_READ),
            @At(value = "FIELD", target = BIOME_FREQUENCY, opcode = Opcodes.GETFIELD, ordinal = MOUNTAIN_Z_READ)})
    private float toroidal$compressMountain(float frequency) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        return frame == null ? frequency : (float) (frequency * frame.compression());
    }

    @WrapOperation(method = APPLY, at = @At(value = "INVOKE",
            target = "Lraccoonman/reterraforged/world/worldgen/cell/continent/Continent;getLandValue(FF)F"))
    private float toroidal$landOnTheWorldLap(Continent continent, float x, float z, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null || frame.compression() == ClimateScaleCompression.NO_COMPRESSION) {
            return original.call(continent, x, z);
        }

        try (RtfLap.Frame.Scope expanded = frame.expand()) {
            return original.call(continent, x, z);
        }
    }

    // Past the warps every climate noise is read at a lattice position, a biome cell's point or a region centre times
    // the biome frequency, so its lap is the lattice's: a cell reached across the seam P cells on reads what it reads
    // from the near side.
    @WrapOperation(method = APPLY, at = @At(value = "INVOKE", target = ReTerraForgedInjectionTargets.NOISE_COMPUTE))
    private float toroidal$latticeLap(Noise noise, float x, float z, int seed, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null || noise == this.warpX || noise == this.warpZ) {
            return original.call(noise, x, z, seed);
        }

        try (RtfLap.Frame.Scope scaled = frame.scale(frame.snappedFrequency(Direction.Axis.X, this.biomeFreq),
                frame.snappedFrequency(Direction.Axis.Z, this.biomeFreq))) {
            return original.call(noise, x, z, seed);
        }
    }
}
