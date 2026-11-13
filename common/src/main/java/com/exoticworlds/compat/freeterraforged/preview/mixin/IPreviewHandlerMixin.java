package com.exoticworlds.compat.freeterraforged.preview.mixin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.preview.CreationShape;
import com.exoticworlds.compat.preview.PreviewHandle;
import com.exoticworlds.compat.preview.ShownShape;
import com.exoticworlds.compat.preview.TerraForgedPreview;

import etcodehome.freeterraforged.client.gui.screen.presetconfig.IPreviewHandler;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;

@Mixin(value = IPreviewHandler.class, remap = false)
public interface IPreviewHandlerMixin {
    @WrapOperation(method = "executeRegenerate",
            at = @At(value = "INVOKE", target = InjectionTargets.COMPLETABLE_FUTURE_SUPPLY_ASYNC))
    private static CompletableFuture<?> exotic_worlds$buildUnderTheChosenShape(Supplier<?> build, Executor executor,
            Operation<CompletableFuture<?>> original, @Local WorldCreationContext settings) {
        return original.call(CreationShape.underRouterBuild(settings, build), executor);
    }

    @WrapOperation(method = "executeRegenerate", at = @At(value = "INVOKE",
            target = "Letcodehome/freeterraforged/client/gui/screen/presetconfig/IPreviewHandler;getZoom()I"))
    private static int exotic_worlds$showTheChosenShape(IPreviewHandler preview, Operation<Integer> original,
            @Local WorldCreationContext settings) {
        ((ShownShape) preview.page()).exotic_worlds$show(settings);
        return original.call(preview);
    }

    @ModifyReturnValue(method = "onPress", at = @At("RETURN"))
    private static Button.OnPress exotic_worlds$pickTheSpawn(Button.OnPress press) {
        return button -> {
            if (!(button instanceof PreviewHandle preview) || !TerraForgedPreview.pickSpawn(preview)) {
                press.onPress(button);
            }
        };
    }
}
