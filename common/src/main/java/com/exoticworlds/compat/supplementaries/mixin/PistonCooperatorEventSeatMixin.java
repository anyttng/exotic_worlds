package com.exoticworlds.compat.supplementaries.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Mixin(targets = "net.mehvahdjukaar.supplementaries.common.misc.block_movement.PistonCooperationLogic")
public class PistonCooperatorEventSeatMixin {
    @ModifyArg(method = "checkExtendTogether", index = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;blockEvent(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;II)V"))
    private static BlockPos exotic_worlds$canonicalCooperatorEvent(BlockPos cooperator,
            @Local(argsOnly = true) Level level) {
        return WorldLoopAttachments.transformerOf(level).fold(cooperator);
    }
}
