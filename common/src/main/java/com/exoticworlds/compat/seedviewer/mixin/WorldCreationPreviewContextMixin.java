package com.exoticworlds.compat.seedviewer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.BinderOrder;
import com.exoticworlds.compat.seedviewer.CreationShape;
import com.exoticworlds.compat.seedviewer.SeedViewerInjectionTargets;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;

import net.acenia.seedviewer.client.screen.createworld.WorldCreationPreviewContext;
import net.acenia.seedviewer.client.screen.createworld.WorldgenEditorState;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;

@Mixin(WorldCreationPreviewContext.class)
public class WorldCreationPreviewContextMixin {
    @Unique
    private static final String PREVIEW_CONTEXT =
            "Lnet/acenia/seedviewer/client/screen/createworld/WorldCreationPreviewContext;";

    @Unique
    private static final String BUILD = "build(Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;"
            + "Lnet/minecraft/client/gui/screens/worldselection/WorldCreationUiState;"
            + "Lnet/acenia/seedviewer/client/screen/createworld/WorldgenEditorState;Z)" + PREVIEW_CONTEXT;

    @Unique
    private static final int WORLDGEN_IDENTITY = 3;

    @WrapMethod(method = BUILD, order = BinderOrder.FOLD)
    private static WorldCreationPreviewContext toroidal$buildForTheChosenShape(CreateWorldScreen screen,
            WorldCreationUiState uiState, WorldgenEditorState editorState, boolean applyEditorEdits,
            Operation<WorldCreationPreviewContext> original) {
        return GenerationTransformerContext.withRouterBuild(CreationShape.fold(uiState.getSettings()),
                () -> original.call(screen, uiState, editorState, applyEditorEdits));
    }

    @ModifyArg(
            method = BUILD,
            at = @At(
                    value = "INVOKE",
                    target = PREVIEW_CONTEXT + SeedViewerInjectionTargets.CONSTRUCTOR + SeedViewerInjectionTargets.CONTEXT_DESCRIPTOR),
            index = WORLDGEN_IDENTITY)
    private static String toroidal$identifyTheChosenShape(String worldgenIdentity,
            @Local(argsOnly = true) WorldCreationUiState uiState) {
        return worldgenIdentity + CreationShape.fingerprint(uiState.getSettings());
    }
}
