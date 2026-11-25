package com.exoticworlds.compat.apprenticecodex.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import jp.aquafactory.apprenticecodex.item.offhand.ExplorersCane;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Mixin(value = ExplorersCane.class, remap = false)
public abstract class ExplorersCaneMixin {
    @ModifyExpressionValue(method = "resolveCompassAngle", at = @At(value = "INVOKE",
            target = "Ljp/aquafactory/apprenticecodex/item/offhand/ExplorersCane$CompassTarget;"
                    + "pos()Lnet/minecraft/core/BlockPos;"))
    private static @Nullable BlockPos exotic_worlds$seatNeedleTargetBesideHolder(@Nullable BlockPos target,
            @Local(argsOnly = true) Level level,
            @Local(argsOnly = true, ordinal = 0) double sourceX,
            @Local(argsOnly = true, ordinal = 1) double sourceZ) {
        if (target == null) {
            return null;
        }

        return NearestCopy.toward(WorldLoopAttachments.wrappedClientBoundsFoldOf(level),
                BlockPos.containing(sourceX, target.getY(), sourceZ), target);
    }
}
