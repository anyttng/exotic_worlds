package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.level.CurrentServer;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

@Mixin(GlobalPos.class)
public class GlobalPosMixin {
    @WrapOperation(
            method = "isCloseEnough",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;distChessboard(Lnet/minecraft/core/Vec3i;)I"))
    private int toroidal$chessboardThroughSeam(BlockPos stored, Vec3i asked, Operation<Integer> original,
            ResourceKey<Level> dimension) {
        return SeamRange.chessboard(
                WorldLoopAttachments.wrappedTransformerOf(CurrentServer.get(), dimension), stored, asked);
    }
}
