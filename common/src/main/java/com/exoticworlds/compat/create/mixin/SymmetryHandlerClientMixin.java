package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.zurrtum.create.client.content.equipment.symmetryWand.SymmetryHandlerClient;
import com.exoticworlds.client.engine.ClientFrame;

import net.minecraft.world.phys.Vec3;

@Mixin(SymmetryHandlerClient.class)
public abstract class SymmetryHandlerClientMixin {
    @Unique
    private static final String MIRROR_POSITION =
            "Lcom/zurrtum/create/infrastructure/component/SymmetryMirror;getPosition()Lnet/minecraft/world/phys/Vec3;";

    @ModifyExpressionValue(method = "onRenderWorld", at = @At(value = "INVOKE", target = MIRROR_POSITION))
    private static Vec3 exotic_worlds$mirrorInTheRenderFrame(Vec3 canonical) {
        return ClientFrame.nearestToCamera(canonical);
    }

    @ModifyExpressionValue(method = "onClientTick", at = @At(value = "INVOKE", target = MIRROR_POSITION))
    private static Vec3 exotic_worlds$mirrorInTheParticleFrame(Vec3 canonical) {
        return ClientFrame.nearestToCamera(canonical);
    }
}
