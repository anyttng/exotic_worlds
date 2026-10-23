package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.hollingsworth.arsnouveau.api.ritual.AbstractRitual;
import com.hollingsworth.arsnouveau.common.ritual.DenySpawnRitual;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.phys.Vec3;

@Mixin(value = DenySpawnRitual.class, remap = false)
public abstract class DenySpawnRitualMixin {
    @WrapOperation(
            method = "denySiege(Lnet/neoforged/neoforge/event/village/VillageSiegeEvent;)Z",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO_SQR))
    private double exotic_worlds$siegeDistanceThroughSeam(Vec3 siege, Vec3 brazier, Operation<Double> original) {
        return SeamRange.sqr(((AbstractRitual) (Object) this).getWorld(), siege, brazier);
    }
}
