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
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.exoticworlds.compat.terraforged.RtfLap;

import net.minecraft.core.Direction;
import raccoonman.reterraforged.concurrent.Resource;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.continent.uplift.UpliftContinentGenerator;

@Mixin(value = UpliftContinentGenerator.class, remap = false)
public abstract class UpliftContinentGeneratorMixin {
    @Unique
    private static final String FREQUENCY =
            "Lraccoonman/reterraforged/world/worldgen/cell/continent/uplift/UpliftContinentGenerator;frequency:F";

    @Unique
    private static final String CENTER_QUOTIENT = "? / this.frequency";

    @Unique
    private static final String EXPRESSION = "MIXINEXTRAS:EXPRESSION";

    @Unique
    private static final String CENTER_WRITTEN = "continentCenterWritten";

    @Unique
    private static final String WINNER_X = "continentWinnerX";

    @Unique
    private static final String WINNER_Z = "continentWinnerZ";

    @Unique
    private static final String X_QUOTIENT = "continentXQuotient";

    @Unique
    private static final String Z_QUOTIENT = "continentZQuotient";

    @Unique
    private static final int X_READ = 0;

    @Unique
    private static final int Z_READ = 1;

    @Unique
    private static final int X_DIVISOR_READ = 2;

    @Unique
    private static final int Z_DIVISOR_READ = 3;

    @Unique
    private static final int X_DIVISION = 0;

    @Unique
    private static final int Z_DIVISION = 1;

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

    // ReTerraForged writes the center here, from a cell search of its own on the unwarped point.
    @WrapMethod(method = "getSmoothVoronoiGradient")
    private float exotic_worlds$canonicalCenter(Cell cell, float rawX, float rawZ, Operation<Float> original,
            @Share(CENTER_WRITTEN) LocalBooleanRef written, @Share(WINNER_X) LocalIntRef winnerX,
            @Share(WINNER_Z) LocalIntRef winnerZ, @Share(X_QUOTIENT) LocalFloatRef xQuotient,
            @Share(Z_QUOTIENT) LocalFloatRef zQuotient) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(cell, rawX, rawZ);
        }

        written.set(false);
        float gradient = original.call(cell, rawX, rawZ);
        if (!written.get()) {
            return gradient;
        }

        int cellX = winnerX.get();
        int cellZ = winnerZ.get();
        int canonicalX = frame.foldX(cellX, cellZ);
        int canonicalZ = frame.foldZ(cellZ);
        if (canonicalX != cellX || canonicalZ != cellZ) {
            try (Resource<Cell> resource = Cell.getResource()) {
                Cell canonical = resource.get();
                canonical.terrain = cell.terrain;
                // The warp and coast noise have reopened octaves by now, so the frame's scale is no longer the lattice's.
                original.call(canonical,
                        rawX + (canonicalX - cellX) / frame.snappedFrequency(Direction.Axis.X, this.frequency),
                        rawZ + (canonicalZ - cellZ) / frame.snappedFrequency(Direction.Axis.Z, this.frequency));
                cell.continentX = canonical.continentX;
                cell.continentZ = canonical.continentZ;
            }
        }

        if (frame.sheared()) {
            cell.continentX = Math.round(frame.outerX(xQuotient.get(), zQuotient.get()));
        }

        return gradient;
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

    @ModifyExpressionValue(method = "getSmoothVoronoiGradient",
            at = @At(value = "FIELD", target = FREQUENCY, opcode = Opcodes.GETFIELD, ordinal = X_DIVISOR_READ))
    private float exotic_worlds$snapDivisorX(float frequency) {
        return RtfLap.snapped(Direction.Axis.X, frequency);
    }

    @ModifyExpressionValue(method = "getSmoothVoronoiGradient",
            at = @At(value = "FIELD", target = FREQUENCY, opcode = Opcodes.GETFIELD, ordinal = Z_DIVISOR_READ))
    private float exotic_worlds$snapDivisorZ(float frequency) {
        return RtfLap.snapped(Direction.Axis.Z, frequency);
    }

    @Definition(id = "frequency", field = FREQUENCY)
    @Expression(CENTER_QUOTIENT)
    @ModifyExpressionValue(method = "getSmoothVoronoiGradient", at = @At(value = EXPRESSION, ordinal = X_DIVISION))
    private float exotic_worlds$keepX(float quotient, @Local(name = "cellX") int cellX,
            @Local(name = "cellY") int cellY, @Share(CENTER_WRITTEN) LocalBooleanRef written,
            @Share(WINNER_X) LocalIntRef winnerX, @Share(WINNER_Z) LocalIntRef winnerZ,
            @Share(X_QUOTIENT) LocalFloatRef xQuotient) {
        written.set(true);
        winnerX.set(cellX);
        winnerZ.set(cellY);
        xQuotient.set(quotient);
        return quotient;
    }

    @Definition(id = "frequency", field = FREQUENCY)
    @Expression(CENTER_QUOTIENT)
    @ModifyExpressionValue(method = "getSmoothVoronoiGradient", at = @At(value = EXPRESSION, ordinal = Z_DIVISION))
    private float exotic_worlds$keepZ(float quotient, @Share(Z_QUOTIENT) LocalFloatRef zQuotient) {
        zQuotient.set(quotient);
        return quotient;
    }
}
