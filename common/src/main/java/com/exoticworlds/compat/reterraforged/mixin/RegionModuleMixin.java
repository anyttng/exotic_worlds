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
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.exoticworlds.compat.terraforged.RtfLap;

import net.minecraft.core.Direction;
import raccoonman.reterraforged.concurrent.Resource;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.terrain.region.RegionModule;

@Mixin(value = RegionModule.class, remap = false)
public abstract class RegionModuleMixin {
    @Unique
    private static final String FREQUENCY =
            "Lraccoonman/reterraforged/world/worldgen/cell/terrain/region/RegionModule;frequency:F";

    @Unique
    private static final String CENTER_QUOTIENT = "? / this.frequency";

    @Unique
    private static final String EXPRESSION = "MIXINEXTRAS:EXPRESSION";

    @Unique
    private static final String WINNER_X = "regionWinnerX";

    @Unique
    private static final String WINNER_Z = "regionWinnerZ";

    @Unique
    private static final String X_QUOTIENT = "regionXQuotient";

    @Unique
    private static final String Z_QUOTIENT = "regionZQuotient";

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
    private float frequency;

    @WrapMethod(method = "apply")
    private void exotic_worlds$closeApply(Cell cell, float x, float z, Operation<Void> original,
            @Share(WINNER_X) LocalIntRef winnerX, @Share(WINNER_Z) LocalIntRef winnerZ,
            @Share(X_QUOTIENT) LocalFloatRef xQuotient, @Share(Z_QUOTIENT) LocalFloatRef zQuotient) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            original.call(cell, x, z);
            return;
        }

        try (RtfLap.Frame.Scope lattice = frame.octave(this.frequency)) {
            float xScale = frame.xScale();
            float zScale = frame.zScale();
            float latticeX = frame.latticeX(x, z);
            float latticeZ = frame.shiftZ(z);
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
                    cell.terrainRegionCenterX = canonical.terrainRegionCenterX;
                    cell.terrainRegionCenterZ = canonical.terrainRegionCenterZ;
                }
            }

            if (frame.sheared()) {
                cell.terrainRegionCenterX = frame.outerX(xQuotient.get(), zQuotient.get());
            }
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

    @ModifyExpressionValue(method = "apply",
            at = @At(value = "FIELD", target = FREQUENCY, opcode = Opcodes.GETFIELD, ordinal = X_DIVISOR_READ))
    private float exotic_worlds$snapDivisorX(float frequency) {
        return RtfLap.snapped(Direction.Axis.X, frequency);
    }

    @ModifyExpressionValue(method = "apply",
            at = @At(value = "FIELD", target = FREQUENCY, opcode = Opcodes.GETFIELD, ordinal = Z_DIVISOR_READ))
    private float exotic_worlds$snapDivisorZ(float frequency) {
        return RtfLap.snapped(Direction.Axis.Z, frequency);
    }

    @Definition(id = "frequency", field = FREQUENCY)
    @Expression(CENTER_QUOTIENT)
    @ModifyExpressionValue(method = "apply", at = @At(value = EXPRESSION, ordinal = X_DIVISION))
    private float exotic_worlds$keepX(float quotient, @Local(name = "cellX") int cellX,
            @Local(name = "cellY") int cellY, @Share(WINNER_X) LocalIntRef winnerX,
            @Share(WINNER_Z) LocalIntRef winnerZ, @Share(X_QUOTIENT) LocalFloatRef xQuotient) {
        winnerX.set(cellX);
        winnerZ.set(cellY);
        xQuotient.set(quotient);
        return quotient;
    }

    @Definition(id = "frequency", field = FREQUENCY)
    @Expression(CENTER_QUOTIENT)
    @ModifyExpressionValue(method = "apply", at = @At(value = EXPRESSION, ordinal = Z_DIVISION))
    private float exotic_worlds$keepZ(float quotient, @Share(Z_QUOTIENT) LocalFloatRef zQuotient) {
        zQuotient.set(quotient);
        return quotient;
    }
}
