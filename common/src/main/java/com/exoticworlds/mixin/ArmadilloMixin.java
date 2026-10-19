package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.phys.AABB;

@Mixin(Armadillo.class)
public class ArmadilloMixin {
    @WrapOperation(method = "isScaredBy", at = @At(value = "INVOKE", target = InjectionTargets.AABB_INTERSECTS))
    private boolean exotic_worlds$scareReachThroughSeam(AABB reach, AABB threatBox, Operation<Boolean> original) {
        Armadillo armadillo = (Armadillo) (Object) this;
        AABB folded = FoldedBoxQuery.toward(armadillo, armadillo.position(), threatBox);
        return original.call(reach, folded);
    }
}
