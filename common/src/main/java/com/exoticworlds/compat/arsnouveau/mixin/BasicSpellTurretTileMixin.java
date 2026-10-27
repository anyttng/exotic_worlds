package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldLoopAttachments;
import com.hollingsworth.arsnouveau.common.block.tile.BasicSpellTurretTile;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(value = BasicSpellTurretTile.class, remap = false)
public abstract class BasicSpellTurretTileMixin {
    @WrapOperation(
            method = "onLastConnection(Lnet/minecraft/core/GlobalPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/player/Player;)Lcom/hollingsworth/arsnouveau/api/item/IWandable$Result;",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_SUBTRACT))
    private BlockPos exotic_worlds$linkDeltaThroughSeam(BlockPos turret, Vec3i stored, Operation<BlockPos> original) {
        BlockPos nearest = WorldLoopAttachments.foldIfLevel(((BlockEntity) (Object) this).getLevel())
                .nearestCopy(turret, new BlockPos(stored));
        return original.call(turret, nearest);
    }
}
