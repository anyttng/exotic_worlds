package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.hollingsworth.arsnouveau.common.entity.WildenChimera;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.phys.Vec3;

@Mixin(value = WildenChimera.ChimeraMoveController.class, remap = false)
public abstract class ChimeraMoveControllerMixin {
    @Shadow
    @Final
    private WildenChimera chimera;

    @WrapOperation(method = "diveTick", at = @At(value = "NEW", target = InjectionTargets.VEC3_NEW))
    private Vec3 exotic_worlds$diveDeltaThroughSeam(double x, double y, double z, Operation<Vec3> original) {
        return SeamAim.foldDelta(this.chimera, original.call(x, y, z));
    }
}
