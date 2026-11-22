package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.fold.CuboidBorder;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.shapes.VoxelShape;

@Mixin(CollisionGetter.class)
public interface CollisionGetterMixin {
    @WrapOperation(
            method = "borderCollision(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)"
                    + "Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At(value = "INVOKE", target = InjectionTargets.WORLD_BORDER_GET_COLLISION_SHAPE))
    private static VoxelShape exotic_worlds$borderWallInColliderFrame(WorldBorder border, Operation<VoxelShape> original,
            @Local(argsOnly = true) Entity source) {
        CuboidBorder cuboid = CuboidBorder.of(border);
        return cuboid == null ? original.call(border) : cuboid.wallAround(source.getX(), source.getZ());
    }
}
