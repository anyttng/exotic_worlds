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
import com.exoticworlds.compat.reterraforged.RtfLap;

import net.minecraft.core.Direction;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.continent.simple.ContinentGenerator;
import raccoonman.reterraforged.world.worldgen.util.PosUtil;

@Mixin(value = ContinentGenerator.class, remap = false)
public abstract class ContinentGeneratorMixin {
    private static final String FREQUENCY =
            "Lraccoonman/reterraforged/world/worldgen/cell/continent/simple/ContinentGenerator;frequency:F";

    private static final String POS_PACK = "Lraccoonman/reterraforged/world/worldgen/util/PosUtil;pack(II)J";

    private static final int X_READ = 0;

    private static final int Z_READ = 1;

    @Shadow
    protected int seed;

    @Shadow
    protected float frequency;

    @Shadow
    private float offsetAlpha;

    @WrapMethod(method = "apply")
    private void toroidal$closeApply(Cell cell, float x, float y, Operation<Void> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            original.call(cell, x, y);
            return;
        }

        try (RtfLap.Frame.Scope lattice = frame.octave(this.frequency)) {
            original.call(cell, frame.latticeX(x, y), frame.shiftZ(y));
        }
    }

    @WrapMethod(method = "getEdgeValue")
    private float toroidal$closeEdge(float x, float y, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(x, y);
        }

        try (RtfLap.Frame.Scope lattice = frame.octave(this.frequency)) {
            return original.call(frame.latticeX(x, y), frame.shiftZ(y));
        }
    }

    @WrapMethod(method = "getNearestCenter")
    private long toroidal$closeCenter(float x, float z, Operation<Long> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(x, z);
        }

        try (RtfLap.Frame.Scope lattice = frame.octave(this.frequency)) {
            return original.call(frame.latticeX(x, z), frame.shiftZ(z));
        }
    }

    @ModifyExpressionValue(method = {"apply", "getEdgeValue", "getNearestCenter"},
            at = @At(value = "FIELD", target = FREQUENCY, opcode = Opcodes.GETFIELD, ordinal = X_READ))
    private float toroidal$snapX(float frequency) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        return frame == null ? frequency : frame.snappedFrequency(Direction.Axis.X, frequency);
    }

    @ModifyExpressionValue(method = {"apply", "getEdgeValue", "getNearestCenter"},
            at = @At(value = "FIELD", target = FREQUENCY, opcode = Opcodes.GETFIELD, ordinal = Z_READ))
    private float toroidal$snapZ(float frequency) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        return frame == null ? frequency : frame.snappedFrequency(Direction.Axis.Z, frequency);
    }

    @WrapOperation(method = "apply", at = @At(value = "FIELD", target = ReTerraForgedInjectionTargets.CELL_CONTINENT_X,
            opcode = Opcodes.PUTFIELD))
    private void toroidal$canonicalCenterX(Cell cell, int value, Operation<Void> original,
            @Local(name = "cellX") int cellX, @Local(name = "cellY") int cellY) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        original.call(cell, frame == null ? value : toroidal$centerX(frame, cellX, cellY));
    }

    @WrapOperation(method = "apply", at = @At(value = "FIELD", target = ReTerraForgedInjectionTargets.CELL_CONTINENT_Z,
            opcode = Opcodes.PUTFIELD))
    private void toroidal$canonicalCenterZ(Cell cell, int value, Operation<Void> original,
            @Local(name = "cellX") int cellX, @Local(name = "cellY") int cellY) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        original.call(cell, frame == null ? value : toroidal$centerZ(frame, cellX, cellY));
    }

    // The winning cell's jittered point lies well inside the cell, so flooring it gives the cell back.
    @WrapOperation(method = "getNearestCenter", at = @At(value = "INVOKE", target = POS_PACK))
    private long toroidal$canonicalNearest(int conX, int conZ, Operation<Long> original,
            @Local(name = "centerX") float centerX, @Local(name = "centerY") float centerY) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(conX, conZ);
        }

        int cellX = (int) Math.floor(centerX);
        int cellY = (int) Math.floor(centerY);
        return PosUtil.pack(toroidal$centerX(frame, cellX, cellY), toroidal$centerZ(frame, cellX, cellY));
    }

    @Unique
    private int toroidal$centerX(RtfLap.Frame frame, int cellX, int cellY) {
        float x = CellCenters.jitteredX(frame, this.seed, cellX, cellY, this.offsetAlpha)
                / frame.snappedFrequency(Direction.Axis.X, this.frequency);
        return (int) (frame.sheared() ? frame.outerX(x, toroidal$blockZ(frame, cellX, cellY)) : x);
    }

    @Unique
    private int toroidal$centerZ(RtfLap.Frame frame, int cellX, int cellY) {
        return (int) toroidal$blockZ(frame, cellX, cellY);
    }

    @Unique
    private float toroidal$blockZ(RtfLap.Frame frame, int cellX, int cellY) {
        return CellCenters.jitteredZ(frame, this.seed, cellX, cellY, this.offsetAlpha)
                / frame.snappedFrequency(Direction.Axis.Z, this.frequency);
    }
}
