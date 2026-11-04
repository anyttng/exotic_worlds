package com.exoticworlds.compat.ultraterraforged.mixin;

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
import com.exoticworlds.compat.terraforged.RtfLap;
import com.exoticworlds.compat.ultraterraforged.CenterAxis;
import com.exoticworlds.compat.ultraterraforged.ContinentWarp;
import com.exoticworlds.compat.ultraterraforged.UltraTerraForgedInjectionTargets;

import net.minecraft.core.Direction;
import com.pandaismyname1.ultraterraforged.concurrent.Resource;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.continent.advanced.AdvancedContinentGenerator;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.domain.Domain;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;

@Mixin(value = AdvancedContinentGenerator.class, remap = false)
public abstract class AdvancedContinentGeneratorMixin {
    @Unique
    private static final String FREQUENCY = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/continent/advanced/"
            + "AdvancedContinentGenerator;frequency:F";

    @Unique
    private static final String CORRECTED_CENTER = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/continent/"
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

        try (RtfLap.Frame.Scope _ = frame.octave(this.frequency)) {
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

    @WrapOperation(method = "apply", at = @At(value = "INVOKE", target = "Lcom/pandaismyname1/ultraterraforged/world/"
            + "worldgen/noise/domain/Domain;getX(FFI)F"))
    private float exotic_worlds$warpX(Domain warp, float x, float z, int seed, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(warp, x, z, seed);
        }

        float xRatio = ContinentWarp.ratio(frame, Direction.Axis.X, this.frequency);
        float zRatio = ContinentWarp.ratio(frame, Direction.Axis.Z, this.frequency);
        try (RtfLap.Frame.Scope _ = ContinentWarp.enter(frame, xRatio, zRatio)) {
            float u = x / xRatio;
            return x + (original.call(warp, u, z / zRatio, seed) - u) * xRatio;
        }
    }

    @WrapOperation(method = "apply", at = @At(value = "INVOKE", target = "Lcom/pandaismyname1/ultraterraforged/world/"
            + "worldgen/noise/domain/Domain;getZ(FFI)F"))
    private float exotic_worlds$warpZ(Domain warp, float x, float z, int seed, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(warp, x, z, seed);
        }

        float xRatio = ContinentWarp.ratio(frame, Direction.Axis.X, this.frequency);
        float zRatio = ContinentWarp.ratio(frame, Direction.Axis.Z, this.frequency);
        try (RtfLap.Frame.Scope _ = ContinentWarp.enter(frame, xRatio, zRatio)) {
            float v = z / zRatio;
            return z + (original.call(warp, x / xRatio, v, seed) - v) * zRatio;
        }
    }

    // The cliff and bay noises read the lattice position, so the lap they see is the lattice's.
    @WrapOperation(method = "apply", at = @At(value = "INVOKE", target = UltraTerraForgedInjectionTargets.NOISE_COMPUTE))
    private float exotic_worlds$latticeLap(Noise noise, float x, float y, int seed, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(noise, x, y, seed);
        }

        try (RtfLap.Frame.Scope _ = frame.scale(frame.snappedFrequency(Direction.Axis.X, this.frequency),
                frame.snappedFrequency(Direction.Axis.Z, this.frequency))) {
            return original.call(noise, x, y, seed);
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
        try (CenterAxis _ = CenterAxis.current().along(Direction.Axis.X)) {
            return original.call(generator, point, average);
        }
    }

    @WrapOperation(method = "apply", at = @At(value = "INVOKE", target = CORRECTED_CENTER, ordinal = Z_CENTER))
    private int exotic_worlds$centerZ(AdvancedContinentGenerator generator, float point, float average,
            Operation<Integer> original) {
        try (CenterAxis _ = CenterAxis.current().along(Direction.Axis.Z)) {
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
