package com.exoticworlds.platform.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.fabricmc.fabric.impl.lookup.block.BlockApiCacheImpl;
import net.fabricmc.fabric.impl.lookup.block.ServerWorldCache;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

@Mixin(BlockApiCacheImpl.class)
public class BlockApiCacheImplMixin {
    @WrapOperation(
            method = "<init>",
            at = @At(value = "INVOKE",
                    target = "Lnet/fabricmc/fabric/impl/lookup/block/ServerWorldCache;fabric_registerCache(Lnet/minecraft/core/BlockPos;Lnet/fabricmc/fabric/impl/lookup/block/BlockApiCacheImpl;)V"))
    private void exotic_worlds$registerUnderCanonicalKey(ServerWorldCache level, BlockPos pos,
            BlockApiCacheImpl<?, ?> cache, Operation<Void> original) {
        original.call(level, WorldLoopAttachments.foldOf((ServerLevel) level).fold(pos), cache);
    }
}
