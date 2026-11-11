package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.content.trains.entity.CarriageContraptionEntity;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;

import net.minecraft.core.Position;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(CarriageContraptionEntity.class)
public class CarriageContraptionEntityMixin {
    @WrapOperation(method = "control", at = @At(value = "INVOKE", target = InjectionTargets.VEC3_CLOSER_THAN))
    private boolean exotic_worlds$controlsReachThroughSeam(Vec3 controls, Position player, double distance,
            Operation<Boolean> original) {
        return original.call(controls, SeamRange.toward((Entity) (Object) this, controls, player), distance);
    }
}
