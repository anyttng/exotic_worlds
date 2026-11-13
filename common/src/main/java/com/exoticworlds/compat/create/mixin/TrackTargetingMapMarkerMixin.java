package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.exoticworlds.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;

@Mixin(targets = "com.zurrtum.create.content.trains.track.TrackTargetingBehaviour")
public class TrackTargetingMapMarkerMixin {
    @ModifyReturnValue(method = "getPositionForMapMarker", at = @At("RETURN"))
    private BlockPos exotic_worlds$canonicalMapMarkerTarget(BlockPos target) {
        return CreateSeamFold.canonicalOnServer(((BlockEntityBehaviour<?>) (Object) this).getLevel(), target);
    }
}
