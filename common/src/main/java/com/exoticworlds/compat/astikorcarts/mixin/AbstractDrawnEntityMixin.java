package com.exoticworlds.compat.astikorcarts.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.jusipat.astikorcartsredux.entity.AbstractDrawnEntity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.exoticworlds.engine.seam.SeamSteering;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = AbstractDrawnEntity.class, remap = false)
public abstract class AbstractDrawnEntityMixin {
    @WrapMethod(method = "getRelativeTargetVec")
    private Vec3 toroidal$pullDeltaThroughSeam(float delta, Operation<Vec3> original) {
        return SeamAim.foldDelta((Entity) (Object) this, original.call(delta));
    }

    @ModifyExpressionValue(method = "shouldRemovePulling",
            at = @At(value = "NEW", target = InjectionTargets.VEC3_NEW, ordinal = 1))
    private Vec3 toroidal$pullerSightThroughSeam(Vec3 puller) {
        return SeamSteering.nearestCopy((Entity) (Object) this, puller);
    }
}
