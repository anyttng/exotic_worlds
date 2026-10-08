package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.reterraforged.RtfLap;

import net.minecraft.core.Direction;
import raccoonman.reterraforged.world.worldgen.cell.continent.advanced.AbstractContinent;

@Mixin(value = AbstractContinent.class, remap = false)
public abstract class AbstractContinentMixin {
    // The default continent is the cell at the origin, and every copy of it one lap away is the same continent.
    @WrapMethod(method = "isDefaultContinent")
    private boolean toroidal$foldDefault(int cellX, int cellY, Operation<Boolean> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(cellX, cellY);
        }

        return original.call(frame.fold(Direction.Axis.X, cellX), frame.fold(Direction.Axis.Z, cellY));
    }
}
