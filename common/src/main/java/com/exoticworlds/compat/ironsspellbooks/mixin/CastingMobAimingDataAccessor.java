package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import io.redspace.ironsspellbooks.spells.CastingMobAimingData;

import net.minecraft.world.phys.Vec3;

@Mixin(value = CastingMobAimingData.class, remap = false)
public interface CastingMobAimingDataAccessor {
    @Invoker("<init>")
    static CastingMobAimingData exotic_worlds$create() {
        throw new AssertionError();
    }

    @Accessor("aimPosition")
    Vec3 exotic_worlds$getAimPosition();

    @Accessor("lastAimPosition")
    Vec3 exotic_worlds$getLastAimPosition();

    @Accessor("aimPosition")
    void exotic_worlds$setAimPosition(Vec3 aimPosition);

    @Accessor("lastAimPosition")
    void exotic_worlds$setLastAimPosition(Vec3 lastAimPosition);
}
