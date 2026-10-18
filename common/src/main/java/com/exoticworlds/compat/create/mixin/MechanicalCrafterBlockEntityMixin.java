package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.exoticworlds.accessors.SeamKeyedBlockEntity;
import com.exoticworlds.compat.create.CrafterGroupFold;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

@Mixin(value = MechanicalCrafterBlockEntity.class, remap = false)
public abstract class MechanicalCrafterBlockEntityMixin implements SeamKeyedBlockEntity {
    @Inject(method = "read", at = @At("RETURN"))
    private void exotic_worlds$normalizeOnRead(CompoundTag compound, HolderLookup.Provider registries,
            boolean clientPacket, CallbackInfo ci) {
        exotic_worlds$normalizeWithLevel();
    }

    @Override
    public void exotic_worlds$rekey() {
        exotic_worlds$normalizeWithLevel();
    }

    @Unique
    private void exotic_worlds$normalizeWithLevel() {
        MechanicalCrafterBlockEntity crafter = (MechanicalCrafterBlockEntity) (Object) this;
        if (!(crafter.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        CrafterGroupFold.normalizeOwn(serverLevel, crafter);
    }
}
