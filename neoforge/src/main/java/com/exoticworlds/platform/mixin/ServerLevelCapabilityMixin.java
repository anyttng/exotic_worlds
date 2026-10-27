package com.exoticworlds.platform.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

@Mixin(ServerLevel.class)
public class ServerLevelCapabilityMixin {
    @ModifyVariable(method = "registerCapabilityListener", at = @At("HEAD"), argsOnly = true)
    private BlockPos exotic_worlds$registerUnderCanonicalKey(BlockPos pos) {
        return exotic_worlds$canonicalKey(pos);
    }

    @ModifyVariable(method = "invalidateCapabilities(Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"), argsOnly = true)
    private BlockPos exotic_worlds$invalidateUnderCanonicalKey(BlockPos pos) {
        return exotic_worlds$canonicalKey(pos);
    }

    @Unique
    private BlockPos exotic_worlds$canonicalKey(BlockPos pos) {
        return WorldLoopAttachments.foldOf((ServerLevel) (Object) this).fold(pos);
    }
}
