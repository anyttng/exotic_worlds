package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import io.redspace.ironsspellbooks.network.casting.SyncCastingMobAimingDataPacket;
import io.redspace.ironsspellbooks.spells.CastingMobAimingData;

@Mixin(value = SyncCastingMobAimingDataPacket.class, remap = false)
public interface SyncCastingMobAimingDataPacketAccessor {
    @Accessor("entityId")
    int exotic_worlds$getEntityId();

    @Accessor("aimingData")
    CastingMobAimingData exotic_worlds$getAimingData();
}
