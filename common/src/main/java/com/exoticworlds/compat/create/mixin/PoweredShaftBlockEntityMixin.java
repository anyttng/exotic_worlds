package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.SeamKeyedBlockEntity;
import com.exoticworlds.compat.create.RelativeKeyFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

@Mixin(value = PoweredShaftBlockEntity.class, remap = false)
public abstract class PoweredShaftBlockEntityMixin implements SeamKeyedBlockEntity {
    @WrapOperation(method = "update", at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_SUBTRACT))
    private BlockPos exotic_worlds$foldStoredKey(BlockPos worldPosition, Vec3i sourcePos, Operation<BlockPos> original) {
        return exotic_worlds$fold(worldPosition, sourcePos, original.call(worldPosition, sourcePos));
    }

    @WrapOperation(method = "isPoweredBy", at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_SUBTRACT))
    private BlockPos exotic_worlds$foldComparedKey(BlockPos worldPosition, Vec3i globalPos, Operation<BlockPos> original) {
        return exotic_worlds$fold(worldPosition, globalPos, original.call(worldPosition, globalPos));
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void exotic_worlds$normalizeOnRead(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket,
            CallbackInfo ci) {
        exotic_worlds$normalizeWithLevel();
    }

    @Override
    public void exotic_worlds$rekey() {
        exotic_worlds$normalizeWithLevel();
    }

    @Unique
    private BlockPos exotic_worlds$fold(BlockPos owner, Vec3i partner, BlockPos rawKey) {
        PoweredShaftBlockEntity self = (PoweredShaftBlockEntity) (Object) this;
        return RelativeKeyFold.shortWay(self.getLevel(), owner, partner, rawKey);
    }

    @Unique
    private void exotic_worlds$normalizeWithLevel() {
        PoweredShaftBlockEntity self = (PoweredShaftBlockEntity) (Object) this;
        BlockPos storedKey = self.enginePos;
        if (storedKey == null || !(self.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos normalizedKey = RelativeKeyFold.normalize(serverLevel, self.getBlockPos(), storedKey);
        if (!normalizedKey.equals(storedKey)) {
            self.enginePos = normalizedKey;
        }
    }
}
