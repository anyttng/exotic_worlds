package com.exoticworlds.compat.cobblemon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(value = EnderpearlItem.class, priority = 1100)
public class EnderPearlLookMixin {
    @WrapOperation(
            method = "isLookingAt",
            at = @At(value = "INVOKE", target = InjectionTargets.AABB_CONTAINS))
    private boolean exotic_worlds$lookThroughSeam(AABB box, Vec3 location, Operation<Boolean> original,
            @Local(argsOnly = true) Player player) {
        AABB seated = FoldedBoxQuery.toward(player, location, box);
        return original.call(seated, location);
    }
}
