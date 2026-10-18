package com.exoticworlds.compat.create.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.exoticworlds.accessors.SeamKeyedBlockEntity;
import com.exoticworlds.compat.create.ControllerFrameFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(value = BeltBlockEntity.class, remap = false)
public abstract class BeltBlockEntityControllerMixin implements SeamKeyedBlockEntity {
    @Shadow
    protected @Nullable BlockPos controller;

    @Shadow
    public int beltLength;

    @ModifyVariable(method = "setController", at = @At("HEAD"), argsOnly = true)
    private BlockPos exotic_worlds$foldIncomingController(BlockPos stored) {
        BlockEntity self = (BlockEntity) (Object) this;
        return ControllerFrameFold.inFrameOf(self.getLevel(), stored);
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void exotic_worlds$foldReadController(CompoundTag compound, Provider registries, boolean clientPacket,
            CallbackInfo ci) {
        exotic_worlds$foldStoredController();
    }

    @Override
    public void exotic_worlds$rekey() {
        exotic_worlds$foldStoredController();
    }

    @Unique
    private void exotic_worlds$foldStoredController() {
        BlockPos stored = this.controller;
        if (stored == null || beltLength == 0) {
            return;
        }

        BlockEntity self = (BlockEntity) (Object) this;
        this.controller = ControllerFrameFold.inFrameOf(self.getLevel(), stored);
    }
}
