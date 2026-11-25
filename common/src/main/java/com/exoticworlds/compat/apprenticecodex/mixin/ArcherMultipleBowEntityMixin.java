package com.exoticworlds.compat.apprenticecodex.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.apprenticecodex.SyncedPositionSeat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import jp.aquafactory.apprenticecodex.spell.archermultiple.ArcherMultipleBowEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = ArcherMultipleBowEntity.class, remap = false)
public abstract class ArcherMultipleBowEntityMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = InjectionTargets.VEC3_NEW))
    private Vec3 exotic_worlds$seatHitBesideBow(Vec3 synced) {
        return SyncedPositionSeat.besideEntity((Entity) (Object) this, synced);
    }
}
