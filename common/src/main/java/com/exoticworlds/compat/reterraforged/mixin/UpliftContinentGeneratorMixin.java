package com.exoticworlds.compat.reterraforged.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.reterraforged.CellCenters;
import com.exoticworlds.compat.reterraforged.ReTerraForgedInjectionTargets;
import com.exoticworlds.compat.reterraforged.RtfLap;

import net.minecraft.core.Direction;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.continent.uplift.UpliftContinentGenerator;

@Mixin(value = UpliftContinentGenerator.class, remap = false)
public abstract class UpliftContinentGeneratorMixin {
    private static final String FREQUENCY =
            "Lraccoonman/reterraforged/world/worldgen/cell/continent/uplift/UpliftContinentGenerator;frequency:F";

    private static final int X_READ = 0;

    private static final int Z_READ = 1;

    @Shadow
    protected float frequency;

    @WrapMethod(method = "apply")
    private void toroidal$closeApply(Cell cell, float x, float y, Operation<Void> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            original.call(cell, x, y);
            return;
        }

        try (RtfLap.Frame.Scope lattice = frame.octave(this.frequency)) {
            original.call(cell, frame.shift(Direction.Axis.X, x), frame.shift(Direction.Axis.Z, y));
        }
    }

    // The coast is shaped at the lattice position, so the lap the cliff and bay noises see is the lattice's.
    @WrapMethod(method = "getCoastalDistanceValue")
    private float toroidal$latticeLap(float x, float y, float distance, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(x, y, distance);
        }

        try (RtfLap.Frame.Scope scaled = frame.scale(frame.snappedFrequency(Direction.Axis.X, this.frequency),
                frame.snappedFrequency(Direction.Axis.Z, this.frequency))) {
            return original.call(x, y, distance);
        }
    }

    @ModifyExpressionValue(method = {"apply", "getSmoothVoronoiGradient"},
            at = @At(value = "FIELD", target = FREQUENCY, opcode = Opcodes.GETFIELD, ordinal = X_READ))
    private float toroidal$snapX(float frequency) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        return frame == null ? frequency : frame.snappedFrequency(Direction.Axis.X, frequency);
    }

    @ModifyExpressionValue(method = {"apply", "getSmoothVoronoiGradient"},
            at = @At(value = "FIELD", target = FREQUENCY, opcode = Opcodes.GETFIELD, ordinal = Z_READ))
    private float toroidal$snapZ(float frequency) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        return frame == null ? frequency : frame.snappedFrequency(Direction.Axis.Z, frequency);
    }

    @WrapOperation(method = "getSmoothVoronoiGradient", at = @At(value = "FIELD",
            target = ReTerraForgedInjectionTargets.CELL_CONTINENT_X, opcode = Opcodes.PUTFIELD))
    private void toroidal$canonicalCenterX(Cell cell, int value, Operation<Void> original,
            @Local(name = "cellX") int cellX, @Local(name = "cellY") int cellY) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            original.call(cell, value);
            return;
        }

        AbstractContinentAccessor continent = (AbstractContinentAccessor) (Object) this;
        original.call(cell, Math.round(CellCenters.centroidX(frame, continent.toroidal$seed(), cellX, cellY,
                continent.toroidal$jitter()) / frame.snappedFrequency(Direction.Axis.X, this.frequency)));
    }

    @WrapOperation(method = "getSmoothVoronoiGradient", at = @At(value = "FIELD",
            target = ReTerraForgedInjectionTargets.CELL_CONTINENT_Z, opcode = Opcodes.PUTFIELD))
    private void toroidal$canonicalCenterZ(Cell cell, int value, Operation<Void> original,
            @Local(name = "cellX") int cellX, @Local(name = "cellY") int cellY) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            original.call(cell, value);
            return;
        }

        AbstractContinentAccessor continent = (AbstractContinentAccessor) (Object) this;
        original.call(cell, Math.round(CellCenters.centroidZ(frame, continent.toroidal$seed(), cellX, cellY,
                continent.toroidal$jitter()) / frame.snappedFrequency(Direction.Axis.Z, this.frequency)));
    }
}
