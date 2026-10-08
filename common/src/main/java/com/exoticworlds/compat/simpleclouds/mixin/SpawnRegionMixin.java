package com.exoticworlds.compat.simpleclouds.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.simpleclouds.SimpleCloudsShapes;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.nonamecrackers2.simpleclouds.common.world.SpawnRegion;

import net.minecraft.core.Direction;

@Mixin(value = SpawnRegion.class, remap = false)
public abstract class SpawnRegionMixin {
    @WrapMethod(method = "intersectsCircle")
    private boolean toroidal$seatCircle(float x, float z, float radius, Operation<Boolean> original) {
        ToroidalShape shape = SimpleCloudsShapes.current();
        if (shape == null) {
            return original.call(x, z, radius);
        }

        SpawnRegion self = (SpawnRegion) (Object) this;
        return original.call((float) shape.nearestCoord(Direction.Axis.X, self.x(), x),
                (float) shape.nearestCoord(Direction.Axis.Z, self.z(), z), radius);
    }

    @WrapMethod(method = "includesPoint")
    private boolean toroidal$seatPoint(int x, int z, Operation<Boolean> original) {
        ToroidalShape shape = SimpleCloudsShapes.current();
        if (shape == null) {
            return original.call(x, z);
        }

        SpawnRegion self = (SpawnRegion) (Object) this;
        return original.call((int) shape.nearestCoord(Direction.Axis.X, self.x(), x),
                (int) shape.nearestCoord(Direction.Axis.Z, self.z(), z));
    }
}
