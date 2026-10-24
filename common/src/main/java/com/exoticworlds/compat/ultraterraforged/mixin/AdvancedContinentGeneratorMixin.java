package com.exoticworlds.compat.ultraterraforged.mixin;

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
import com.exoticworlds.compat.ultraterraforged.CellCenters;
import com.exoticworlds.compat.ultraterraforged.UltraTerraForgedInjectionTargets;
import com.exoticworlds.compat.terraforged.RtfLap;

import net.minecraft.core.Direction;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.continent.advanced.AdvancedContinentGenerator;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;

@Mixin(value = AdvancedContinentGenerator.class, remap = false)
public abstract class AdvancedContinentGeneratorMixin {
    @Unique
    private static final String FREQUENCY = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/continent/advanced/"
            + "AdvancedContinentGenerator;frequency:F";

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

    // The cliff and bay noises read the lattice position, so the lap they see is the lattice's.
    @WrapOperation(method = "apply", at = @At(value = "INVOKE", target = UltraTerraForgedInjectionTargets.NOISE_COMPUTE))
    private float exotic_worlds$latticeLap(Noise noise, float x, float y, int seed, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(noise, x, y, seed);
        }

        try (RtfLap.Frame.Scope scaled = frame.scale(frame.snappedFrequency(Direction.Axis.X, this.frequency),
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

    @WrapOperation(method = "apply", at = @At(value = "FIELD", target = UltraTerraForgedInjectionTargets.CELL_CONTINENT_X,
            opcode = Opcodes.PUTFIELD))
    private void exotic_worlds$canonicalCenterX(Cell cell, int value, Operation<Void> original,
            @Local(name = "cellX") int cellX, @Local(name = "cellY") int cellY) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            original.call(cell, value);
            return;
        }

        AbstractContinentAccessor continent = (AbstractContinentAccessor) (Object) this;
        original.call(cell, (int) CellCenters.blockX(frame, CellCenters.Center.CORRECTED, continent.exotic_worlds$seed(),
                cellX, cellY, continent.exotic_worlds$jitter(), this.frequency));
    }

    @WrapOperation(method = "apply", at = @At(value = "FIELD", target = UltraTerraForgedInjectionTargets.CELL_CONTINENT_Z,
            opcode = Opcodes.PUTFIELD))
    private void exotic_worlds$canonicalCenterZ(Cell cell, int value, Operation<Void> original,
            @Local(name = "cellX") int cellX, @Local(name = "cellY") int cellY) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            original.call(cell, value);
            return;
        }

        AbstractContinentAccessor continent = (AbstractContinentAccessor) (Object) this;
        original.call(cell, (int) CellCenters.blockZ(frame, CellCenters.Center.CORRECTED, continent.exotic_worlds$seed(),
                cellX, cellY, continent.exotic_worlds$jitter(), this.frequency));
    }
}
