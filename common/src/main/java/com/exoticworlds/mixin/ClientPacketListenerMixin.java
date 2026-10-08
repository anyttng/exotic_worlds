package com.exoticworlds.mixin;

import java.util.Map;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.client.engine.PublishedShapes;
import com.exoticworlds.client.engine.SyncedTagFold;
import com.exoticworlds.engine.net.ComponentPositions;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "clearLevel", at = @At("RETURN"))
    private void toroidal$forgetWhatTheServerPublished(CallbackInfo ci) {
        PublishedShapes.clear();
        SyncedTagFold.declare(Map.of());
        ComponentPositions.declare(Set.of());
    }

    @WrapOperation(
            method = "lambda$handleBlockEntityData$0",
            at = @At(value = "INVOKE", target = InjectionTargets.TAG_VALUE_INPUT_CREATE))
    private ValueInput toroidal$seatSyncedPositions(ProblemReporter reporter, HolderLookup.Provider registries,
            CompoundTag tag, Operation<ValueInput> original, @Local(argsOnly = true) BlockEntity blockEntity) {
        return original.call(reporter, registries, SyncedTagFold.inFrameOf(blockEntity, tag));
    }
}
