package com.exoticworlds.compat.apprenticecodex.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.apprenticecodex.SyncedPositionSeat;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import jp.aquafactory.apprenticecodex.spell.shock.ShockBoltEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = ShockBoltEntity.class, remap = false)
public abstract class ShockBoltEntityMixin {
    @ModifyReturnValue(method = "getEndPosition", at = @At("RETURN"))
    private Vec3 exotic_worlds$seatEndBesideBolt(Vec3 synced) {
        return SyncedPositionSeat.besideEntity((Entity) (Object) this, synced);
    }
}
