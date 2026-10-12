package com.exoticworlds.compat.freeterraforged.preview.mixin;

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

import etcodehome.freeterraforged.client.gui.screen.presetconfig.IPreviewHandler;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;

@Mixin(value = IPreviewHandler.class, remap = false)
public interface IPreviewHandlerMixin {
    @WrapOperation(method = "executeRegenerate",
            at = @At(value = "INVOKE", target = InjectionTargets.COMPLETABLE_FUTURE_SUPPLY_ASYNC))
    private static CompletableFuture<?> toroidal$buildUnderTheChosenShape(Supplier<?> build, Executor executor,
            Operation<CompletableFuture<?>> original, @Local WorldCreationContext settings) {
        return original.call(CreationShape.underRouterBuild(settings, build), executor);
    }
}
