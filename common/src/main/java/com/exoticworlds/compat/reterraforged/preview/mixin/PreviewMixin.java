package com.exoticworlds.compat.reterraforged.preview.mixin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.CreationShape;

import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import raccoonman.reterraforged.client.gui.screen.presetconfig.Preview2D;
import raccoonman.reterraforged.client.gui.screen.presetconfig.Preview3D;

@Mixin(value = {Preview2D.class, Preview3D.class}, remap = false)
public abstract class PreviewMixin {
    @WrapOperation(method = "executeRegenerate",
            at = @At(value = "INVOKE", target = InjectionTargets.COMPLETABLE_FUTURE_SUPPLY_ASYNC))
    private CompletableFuture<?> toroidal$buildUnderTheChosenShape(Supplier<?> build, Executor executor,
            Operation<CompletableFuture<?>> original, @Local WorldCreationContext settings) {
        return original.call(CreationShape.underRouterBuild(settings, build), executor);
    }
}
