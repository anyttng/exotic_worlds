package com.exoticworlds.compat.create.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.foundation.utility.RaycastHelper;
import com.exoticworlds.compat.create.CreateSeamFold;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

@Mixin(value = RaycastHelper.class, remap = false)
public class RaycastHelperMixin {
    @ModifyReturnValue(method = "rayTraceRange", at = @At("RETURN"))
    private static @Nullable BlockHitResult exotic_worlds$canonicaliseHit(@Nullable BlockHitResult hit, Level level) {
        return hit == null ? null : CreateSeamFold.canonicalOnServer(level, hit);
    }
}
