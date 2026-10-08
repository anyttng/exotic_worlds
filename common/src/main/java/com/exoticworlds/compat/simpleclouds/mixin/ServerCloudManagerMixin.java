package com.exoticworlds.compat.simpleclouds.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.compat.simpleclouds.SimpleCloudsShapes;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.nonamecrackers2.simpleclouds.common.world.ServerCloudManager;

@Mixin(value = ServerCloudManager.class, remap = false)
public abstract class ServerCloudManagerMixin {
    @WrapMethod(method = "attemptToSpawnLightning")
    private void toroidal$bindCloudShape(Operation<Void> original) {
        SimpleCloudsShapes.bound(((CloudManagerAccessor) this).toroidal$level(), () -> original.call());
    }
}
