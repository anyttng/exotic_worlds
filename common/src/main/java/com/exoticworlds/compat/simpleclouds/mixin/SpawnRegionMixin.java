package com.exoticworlds.compat.simpleclouds.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.compat.simpleclouds.CloudShape;
import com.exoticworlds.compat.simpleclouds.SimpleCloudsShapes;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.nonamecrackers2.simpleclouds.common.world.SpawnRegion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

@Mixin(value = SpawnRegion.class, remap = false)
public abstract class SpawnRegionMixin {
    @WrapMethod(method = "intersectsCircle")
    private boolean toroidal$seatCircle(float x, float z, float radius, Operation<Boolean> original) {
        CloudShape shape = SimpleCloudsShapes.current();
        if (shape == null) {
            return original.call(x, z, radius);
        }

        SpawnRegion self = (SpawnRegion) (Object) this;
        Vec3 seated = shape.shape().nearestCopy(new Vec3(self.x(), 0.0, self.z()), new Vec3(x, 0.0, z));
        return original.call((float) seated.x, (float) seated.z, radius);
    }

    @WrapMethod(method = "includesPoint")
    private boolean toroidal$seatPoint(int x, int z, Operation<Boolean> original) {
        CloudShape shape = SimpleCloudsShapes.current();
        if (shape == null) {
            return original.call(x, z);
        }

        SpawnRegion self = (SpawnRegion) (Object) this;
        BlockPos seated = shape.shape().nearestCopy(new BlockPos(self.x(), 0, self.z()), new BlockPos(x, 0, z));
        return original.call(seated.getX(), seated.getZ());
    }
}
