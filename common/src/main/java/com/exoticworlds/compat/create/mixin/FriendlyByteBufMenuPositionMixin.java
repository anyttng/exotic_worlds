package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.exoticworlds.compat.create.client.CreateMenuFrame;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

@Mixin(FriendlyByteBuf.class)
public class FriendlyByteBufMenuPositionMixin {
    // BlockPos.STREAM_CODEC decodes through this static; the instance readBlockPos leaves codec positions unfolded.
    @ModifyReturnValue(
            method = "readBlockPos(Lio/netty/buffer/ByteBuf;)Lnet/minecraft/core/BlockPos;",
            at = @At("RETURN"))
    private static BlockPos exotic_worlds$foldMenuPayloadPosition(BlockPos canonical) {
        return CreateMenuFrame.fold(canonical);
    }
}
