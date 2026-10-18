package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.NearestCopy;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "net/minecraft/server/level/ChunkMap$TrackedEntity")
public class TrackedEntityMixin {
    @Shadow
    @Final
    Entity entity;

    @WrapOperation(
            method = "updatePlayer",
            at = @At(
                    value = "INVOKE",
                    target = InjectionTargets.VEC3_SUBTRACT))
    private Vec3 exotic_worlds$deltaThroughSeam(Vec3 playerPosition, Vec3 entityPosition, Operation<Vec3> original) {
        WorldFold transformer = ((TransformerSource) this.entity).exotic_worlds$wrappedTransformer();
        return original.call(NearestCopy.toward(transformer, entityPosition, playerPosition), entityPosition);
    }
}
