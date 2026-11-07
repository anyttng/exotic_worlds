package com.exoticworlds.compat.reterraforged.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.exoticworlds.compat.terraforged.CenterAxis;
import com.exoticworlds.compat.terraforged.RtfLap;

import net.minecraft.core.Direction;
import raccoonman.reterraforged.concurrent.Resource;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.continent.advanced.AdvancedContinentGenerator;

@Mixin(value = AdvancedContinentGenerator.class, remap = false)
public abstract class AdvancedContinentGeneratorMixin {
    @Unique
    private static final String FREQUENCY = "Lraccoonman/reterraforged/world/worldgen/cell/continent/advanced/"
            + "AdvancedContinentGenerator;frequency:F";

    @Unique
    private static final String CORRECTED_CENTER = "Lraccoonman/reterraforged/world/worldgen/cell/continent/"
            + "advanced/AdvancedContinentGenerator;getCorrectedContinentCenter(FF)I";

    @Unique
    private static final String WINNER_X = "continentWinnerX";

    @Unique
    private static final String WINNER_Z = "continentWinnerZ";

    @Unique
    private static final int X_READ = 0;

    @Unique
    private static final int Z_READ = 1;

    @Unique
    private static final int X_CENTER = 0;

    @Unique
    private static final int Z_CENTER = 1;

    @Shadow
    protected float frequency;

    @WrapMethod(method = "apply")
    private void exotic_worlds$closeApply(Cell cell, float x, float y, Operation<Void> original,
            @Share(WINNER_X) LocalIntRef winnerX, @Share(WINNER_Z) LocalIntRef winnerZ) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            original.call(cell, x, y);
            return;
        }

        try (RtfLap.Frame.Scope lattice = frame.octave(this.frequency)) {
            float xScale = frame.xScale();
            float zScale = frame.zScale();
            float latticeX = frame.latticeX(x, y);
            float latticeZ = frame.shiftZ(y);
            original.call(cell, latticeX, latticeZ);
            int cellX = winnerX.get();
            int cellZ = winnerZ.get();
            int canonicalX = frame.foldX(cellX, cellZ);
            int canonicalZ = frame.foldZ(cellZ);
            if (canonicalX != cellX || canonicalZ != cellZ) {
                try (Resource<Cell> resource = Cell.getResource()) {
                    Cell canonical = resource.get();
                    original.call(canonical, latticeX + (canonicalX - cellX) / xScale,
                            latticeZ + (canonicalZ - cellZ) / zScale);
                    cell.continentX = canonical.continentX;
                    cell.continentZ = canonical.continentZ;
                }
            }

            if (frame.sheared()) {
                CenterAxis axis = CenterAxis.current();
                cell.continentX = (int) frame.outerX(axis.xQuotient(), axis.zQuotient());
            }
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

    @ModifyExpressionValue(method = "apply",
            at = @At(value = "FIELD", target = FREQUENCY, opcode = Opcodes.GETFIELD, ordinal = X_READ))
    private float exotic_worlds$snapX(float frequency) {
        return RtfLap.snapped(Direction.Axis.X, frequency);
    }

    @ModifyExpressionValue(method = "apply",
            at = @At(value = "FIELD", target = FREQUENCY, opcode = Opcodes.GETFIELD, ordinal = Z_READ))
    private float exotic_worlds$snapZ(float frequency) {
        return RtfLap.snapped(Direction.Axis.Z, frequency);
    }

    @WrapOperation(method = "apply", at = @At(value = "INVOKE", target = CORRECTED_CENTER, ordinal = X_CENTER))
    private int exotic_worlds$centerX(AdvancedContinentGenerator generator, float point, float average,
            Operation<Integer> original, @Local(name = "cellX") int cellX, @Local(name = "cellY") int cellY,
            @Share(WINNER_X) LocalIntRef winnerX, @Share(WINNER_Z) LocalIntRef winnerZ) {
        winnerX.set(cellX);
        winnerZ.set(cellY);
        try (CenterAxis along = CenterAxis.current().along(Direction.Axis.X)) {
            return original.call(generator, point, average);
        }
    }

    @WrapOperation(method = "apply", at = @At(value = "INVOKE", target = CORRECTED_CENTER, ordinal = Z_CENTER))
    private int exotic_worlds$centerZ(AdvancedContinentGenerator generator, float point, float average,
            Operation<Integer> original) {
        try (CenterAxis along = CenterAxis.current().along(Direction.Axis.Z)) {
            return original.call(generator, point, average);
        }
    }

    @ModifyExpressionValue(method = "getCorrectedContinentCenter",
            at = @At(value = "FIELD", target = FREQUENCY, opcode = Opcodes.GETFIELD))
    private float exotic_worlds$snapAlongAxis(float frequency) {
        return CenterAxis.current().snapped(frequency);
    }

    @Definition(id = "frequency", field = FREQUENCY)
    @Expression("? / this.frequency")
    @ModifyExpressionValue(method = "getCorrectedContinentCenter", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
    private float exotic_worlds$keepQuotient(float quotient) {
        return CenterAxis.current().keep(quotient);
    }
}
