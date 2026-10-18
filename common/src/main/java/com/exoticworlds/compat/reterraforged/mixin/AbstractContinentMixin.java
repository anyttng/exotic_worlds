package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.reterraforged.RiverReach;
import com.exoticworlds.compat.reterraforged.RtfLap;

import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings;
import raccoonman.reterraforged.world.worldgen.cell.continent.SimpleContinent;
import raccoonman.reterraforged.world.worldgen.cell.continent.advanced.AbstractContinent;

@Mixin(value = AbstractContinent.class, remap = false)
public abstract class AbstractContinentMixin {
    @Shadow
    protected WorldSettings.ControlPoints controlPoints;

    // The default continent is the cell at the origin, and every copy of it one lap away is the same continent.
    @WrapMethod(method = "isDefaultContinent")
    private boolean exotic_worlds$foldDefault(int cellX, int cellY, Operation<Boolean> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(cellX, cellY);
        }

        return original.call(frame.foldX(cellX, cellY), frame.foldZ(cellY));
    }

    @WrapMethod(method = "getDistanceToOcean")
    private float exotic_worlds$firstCoast(int x, int z, float dx, float dz, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        float reach = frame == null ? RiverReach.UNBOUNDED : RiverReach.reach(frame, dx, dz);
        if (reach == RiverReach.UNBOUNDED) {
            return original.call(x, z, dx, dz);
        }

        return RiverReach.distanceToOcean((SimpleContinent) (Object) this, this.controlPoints.shallowOcean, x, z, dx, dz,
                reach);
    }
}
