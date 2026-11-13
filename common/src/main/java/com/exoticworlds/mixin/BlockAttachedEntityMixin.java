package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;

@Mixin(BlockAttachedEntity.class)
public class BlockAttachedEntityMixin {
    @ModifyExpressionValue(
            method = "readAdditionalSaveData",
            at = @At(value = "NEW", target = "(III)Lnet/minecraft/core/BlockPos;"))
    private BlockPos exotic_worlds$storedAnchorNearestEntity(BlockPos stored) {
        BlockAttachedEntity entity = (BlockAttachedEntity) (Object) this;
        @Nullable WorldFold fold = WorldLoopAttachments.wrappedFoldOfReader(entity.level());
        return NearestCopy.toward(fold, entity.blockPosition(), stored);
    }
}
