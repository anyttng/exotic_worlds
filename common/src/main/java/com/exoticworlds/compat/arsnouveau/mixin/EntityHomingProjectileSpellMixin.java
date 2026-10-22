package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.exoticworlds.engine.seam.SeamSteering;
import com.hollingsworth.arsnouveau.common.entity.EntityHomingProjectileSpell;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

@Mixin(value = EntityHomingProjectileSpell.class, remap = false)
public abstract class EntityHomingProjectileSpellMixin {
    @WrapOperation(method = "tickNextPosition()V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"))
    private AABB exotic_worlds$targetBoxThroughSeam(LivingEntity target, Operation<AABB> original) {
        Entity projectile = (Entity) (Object) this;
        return FoldedBoxQuery.toward(projectile, projectile.position(), original.call(target));
    }

    @ModifyArg(method = "tickNextPosition()V", at = @At(value = "INVOKE",
            target = "Lcom/hollingsworth/arsnouveau/common/entity/EntityHomingProjectileSpell;homeTo(Lnet/minecraft/core/BlockPos;)V"))
    private BlockPos exotic_worlds$homeTargetThroughSeam(BlockPos target) {
        return SeamSteering.nearestCopy((Entity) (Object) this, target);
    }
}
