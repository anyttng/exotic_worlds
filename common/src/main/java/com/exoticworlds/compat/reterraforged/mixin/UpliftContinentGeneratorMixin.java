package com.exoticworlds.compat.reterraforged.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.reterraforged.CellCenters;
import com.exoticworlds.compat.reterraforged.ReTerraForgedInjectionTargets;
import com.exoticworlds.compat.terraforged.RtfLap;

import net.minecraft.core.Direction;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.continent.uplift.UpliftContinentGenerator;

@Mixin(value = UpliftContinentGenerator.class, remap = false)
public abstract class UpliftContinentGeneratorMixin {
    @Unique
    private static final String FREQUENCY =
            "Lraccoonman/reterraforged/world/worldgen/cell/continent/uplift/UpliftContinentGenerator;frequency:F";

    @Unique
    private static final int X_READ = 0;

    @Unique
    private static final int Z_READ = 1;

    @Shadow
    protected float frequency;

    @WrapMethod(method = "apply")
    private void exotic_worlds$closeApply(Cell cell, float x, float y, Operation<Void> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            original.call(cell, x, y);
            return;
        }

        try (RtfLap.Frame.Scope lattice = frame.octave(this.frequency)) {
            original.call(cell, frame.latticeX(x, y), frame.shiftZ(y));
        }
    }

    // The coast is shaped at the lattice position, so the lap the cliff and bay noises see is the lattice's.
    @WrapMethod(method = "getCoastalDistanceValue")
    private float exotic_worlds$latticeLap(float x, float y, float distance, Operation<Float> original) {
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
    private float exotic_worlds$snapX(float frequency) {
        return RtfLap.snapped(Direction.Axis.X, frequency);
    }

    @ModifyExpressionValue(method = {"apply", "getSmoothVoronoiGradient"},
            at = @At(value = "FIELD", target = FREQUENCY, opcode = Opcodes.GETFIELD, ordinal = Z_READ))
    private float exotic_worlds$snapZ(float frequency) {
        return RtfLap.snapped(Direction.Axis.Z, frequency);
    }

    @WrapOperation(method = "getSmoothVoronoiGradient", at = @At(value = "FIELD",
            target = ReTerraForgedInjectionTargets.CELL_CONTINENT_X, opcode = Opcodes.PUTFIELD))
    private void exotic_worlds$canonicalCenterX(Cell cell, int value, Operation<Void> original,
            @Local(name = "cellX") int cellX, @Local(name = "cellY") int cellY) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            original.call(cell, value);
            return;
        }

        AbstractContinentAccessor continent = (AbstractContinentAccessor) (Object) this;
        original.call(cell, Math.round(CellCenters.blockX(frame, CellCenters.Center.CENTROID,
                continent.exotic_worlds$seed(), cellX, cellY, continent.exotic_worlds$jitter(), this.frequency)));
    }

    @WrapOperation(method = "getSmoothVoronoiGradient", at = @At(value = "FIELD",
            target = ReTerraForgedInjectionTargets.CELL_CONTINENT_Z, opcode = Opcodes.PUTFIELD))
    private void exotic_worlds$canonicalCenterZ(Cell cell, int value, Operation<Void> original,
            @Local(name = "cellX") int cellX, @Local(name = "cellY") int cellY) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            original.call(cell, value);
            return;
        }

        AbstractContinentAccessor continent = (AbstractContinentAccessor) (Object) this;
        original.call(cell, Math.round(CellCenters.blockZ(frame, CellCenters.Center.CENTROID,
                continent.exotic_worlds$seed(), cellX, cellY, continent.exotic_worlds$jitter(), this.frequency)));
    }
}
