package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldLoopAttachments;
import com.hollingsworth.arsnouveau.common.block.tile.RotatingTurretTile;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = RotatingTurretTile.class, remap = false)
public abstract class RotatingTurretTileMixin {
    @ModifyExpressionValue(
            method = "aim(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)V",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_AT_CENTER_OF, ordinal = 1))
    private Vec3 exotic_worlds$aimThroughSeam(Vec3 target, @Local(ordinal = 0) Vec3 turret) {
        return WorldLoopAttachments.foldIfLevel(((BlockEntity) (Object) this).getLevel())
                .nearestCopy(turret, target);
    }
}
