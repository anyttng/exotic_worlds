package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import io.redspace.ironsspellbooks.block.BloodCauldronBlock;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

@Mixin(value = BloodCauldronBlock.class, remap = false)
public abstract class BloodCauldronBlockMixin {

    @WrapOperation(
            method = "attemptCookEntity(Lnet/minecraft/world/level/block/state/BlockState;"
                    + "Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;"
                    + "Lio/redspace/ironsspellbooks/block/BloodCauldronBlock$CookExecution;)V",
            at = @At(value = "INVOKE", target = InjectionTargets.AABB_INTERSECTS))
    private static boolean exotic_worlds$touchThroughSeam(AABB entityBox, AABB shapeBox, Operation<Boolean> original,
            @Local(argsOnly = true) Entity entity) {
        return original.call(entityBox, FoldedBoxQuery.toward(entity, entity.position(), shapeBox));
    }
}
