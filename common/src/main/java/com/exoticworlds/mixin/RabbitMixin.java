package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.phys.Vec3;

@Mixin(Rabbit.class)
public class RabbitMixin {
    @WrapMethod(method = "facePoint(DD)V")
    private void exotic_worlds$facePointThroughSeam(double faceX, double faceZ, Operation<Void> original) {
        Rabbit rabbit = (Rabbit) (Object) this;
        Vec3 nearest = SeamSteering.nearestCopy(rabbit, new Vec3(faceX, rabbit.getY(), faceZ));
        original.call(nearest.x, nearest.z);
    }
}
