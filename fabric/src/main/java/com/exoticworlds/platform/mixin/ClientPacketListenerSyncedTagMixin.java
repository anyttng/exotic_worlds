package com.exoticworlds.platform.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.client.engine.SyncedTagFold;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerSyncedTagMixin {
    @WrapOperation(
            method = "method_38542",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_ENTITY_LOAD_WITH_COMPONENTS))
    private void toroidal$seatSyncedPositions(BlockEntity blockEntity, CompoundTag tag,
            HolderLookup.Provider registries, Operation<Void> original) {
        original.call(blockEntity, SyncedTagFold.inFrameOf(blockEntity, tag), registries);
    }
}
