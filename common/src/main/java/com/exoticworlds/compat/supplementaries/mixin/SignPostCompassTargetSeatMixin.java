package com.exoticworlds.compat.supplementaries.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

@Mixin(targets = "net.mehvahdjukaar.supplementaries.common.block.tiles.SignPostBlockTile")
public class SignPostCompassTargetSeatMixin {
    @WrapOperation(method = "handleInteraction", at = @At(value = "INVOKE",
            target = "Lnet/mehvahdjukaar/supplementaries/common/block/tiles/SignPostBlockTile;getCompassTrackedPos(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/core/BlockPos;"))
    private @Nullable BlockPos exotic_worlds$targetNearestSign(ServerLevel level, ItemStack compass,
            Operation<BlockPos> original, @Local(argsOnly = true) BlockPos signPos) {
        BlockPos target = original.call(level, compass);
        return target == null ? null : WorldLoopAttachments.transformerOf(level).nearestCopy(signPos, target);
    }
}
