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
import com.exoticworlds.compat.reterraforged.RtfLap;

import net.minecraft.core.Direction;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.terrain.region.RegionModule;

@Mixin(value = RegionModule.class, remap = false)
public abstract class RegionModuleMixin {
    private static final String FREQUENCY =
            "Lraccoonman/reterraforged/world/worldgen/cell/terrain/region/RegionModule;frequency:F";

    private static final String CENTER_X = "Lraccoonman/reterraforged/world/worldgen/cell/Cell;terrainRegionCenterX:F";

    private static final String CENTER_Z = "Lraccoonman/reterraforged/world/worldgen/cell/Cell;terrainRegionCenterZ:F";

    private static final float REGION_JITTER = 0.7F;

    private static final int X_READ = 0;

    private static final int Z_READ = 1;

    @Shadow
    private int seed;

    @Shadow
    private float frequency;

    @WrapMethod(method = "apply")
    private void toroidal$closeApply(Cell cell, float x, float z, Operation<Void> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            original.call(cell, x, z);
            return;
        }

        try (RtfLap.Frame.Scope lattice = frame.octave(this.frequency)) {
            original.call(cell, frame.latticeX(x, z), frame.shiftZ(z));
        }
    }

    @ModifyExpressionValue(method = "apply",
            at = @At(value = "FIELD", target = FREQUENCY, opcode = Opcodes.GETFIELD, ordinal = X_READ))
    private float toroidal$snapX(float frequency) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        return frame == null ? frequency : frame.snappedFrequency(Direction.Axis.X, frequency);
    }

    @ModifyExpressionValue(method = "apply",
            at = @At(value = "FIELD", target = FREQUENCY, opcode = Opcodes.GETFIELD, ordinal = Z_READ))
    private float toroidal$snapZ(float frequency) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        return frame == null ? frequency : frame.snappedFrequency(Direction.Axis.Z, frequency);
    }

    @WrapOperation(method = "apply", at = @At(value = "FIELD", target = CENTER_X, opcode = Opcodes.PUTFIELD))
    private void toroidal$canonicalCenterX(Cell cell, float value, Operation<Void> original,
            @Local(name = "cellX") int cellX, @Local(name = "cellY") int cellY) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            original.call(cell, value);
            return;
        }

        float x = CellCenters.jitteredX(frame, this.seed, cellX, cellY, REGION_JITTER)
                / frame.snappedFrequency(Direction.Axis.X, this.frequency);
        original.call(cell, frame.sheared() ? frame.outerX(x, toroidal$blockZ(frame, cellX, cellY)) : x);
    }

    @WrapOperation(method = "apply", at = @At(value = "FIELD", target = CENTER_Z, opcode = Opcodes.PUTFIELD))
    private void toroidal$canonicalCenterZ(Cell cell, float value, Operation<Void> original,
            @Local(name = "cellX") int cellX, @Local(name = "cellY") int cellY) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        original.call(cell, frame == null ? value : toroidal$blockZ(frame, cellX, cellY));
    }

    @Unique
    private float toroidal$blockZ(RtfLap.Frame frame, int cellX, int cellY) {
        return CellCenters.jitteredZ(frame, this.seed, cellX, cellY, REGION_JITTER)
                / frame.snappedFrequency(Direction.Axis.Z, this.frequency);
    }
}
