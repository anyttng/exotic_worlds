package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.hollingsworth.arsnouveau.common.entity.goal.lily.IAdorable;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

@Mixin(value = IAdorable.class, remap = false)
public interface IAdorableMixin {
    @WrapOperation(method = "isLookingAtMe", at = @At(value = "NEW", target = InjectionTargets.VEC3_NEW))
    private Vec3 exotic_worlds$gazeDeltaThroughSeam(double x, double y, double z, Operation<Vec3> original,
            @Local(argsOnly = true) Player viewer) {
        return SeamAim.foldDelta(viewer, original.call(x, y, z));
    }
}
