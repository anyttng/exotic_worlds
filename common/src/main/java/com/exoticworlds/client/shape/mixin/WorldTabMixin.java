package com.exoticworlds.client.shape.mixin;

import com.exoticworlds.api.v1.client.ShapeCustomizers;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.shape.WorldShape;
import com.exoticworlds.shape.WorldShapes;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.dimension.LevelStem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$WorldTab")
public class WorldTabMixin {
    @Unique
    private static final Component exotic_worlds$SHAPE_LABEL = Component.translatable("gui.exotic_worlds.world_shape");

    @Unique
    private static final Component exotic_worlds$CUSTOMIZE_LABEL = Component.translatable("selectWorld.customizeType");

    @Unique
    private static final Component exotic_worlds$SEAM_WARNING =
            Component.translatable("gui.exotic_worlds.world_shape.seam_warning");

    @Unique
    private static final int exotic_worlds$SHAPE_BUTTON_WIDTH = 150;

    @Unique
    private static final int exotic_worlds$SHAPE_BUTTON_HEIGHT = 20;

    @Unique
    private CycleButton<WorldShape> exotic_worlds$shapeButton;

    @Unique
    private Button exotic_worlds$customizeShapeButton;

    @Unique
    private boolean exotic_worlds$terrainTilesAtSeam = true;

    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/CommonLayouts;labeledElement(Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/gui/layouts/LayoutElement;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/client/gui/layouts/Layout;",
                    ordinal = 0))
    private void exotic_worlds$addWorldShapeRow(CallbackInfo ci, @Local GridLayout.RowHelper helper,
            @Local(argsOnly = true) CreateWorldScreen screen) {
        this.exotic_worlds$shapeButton = helper.addChild(CycleButton.builder(WorldShape::label, WorldShapes.selected())
                .withValues(WorldShapes.shapes())
                .withTooltip(this::exotic_worlds$tooltipFor)
                .create(0, 0, exotic_worlds$SHAPE_BUTTON_WIDTH, exotic_worlds$SHAPE_BUTTON_HEIGHT, exotic_worlds$SHAPE_LABEL,
                        (button, shape) -> {
                            WorldShapes.select(shape);
                            exotic_worlds$refreshCustomizeButton();
                        }));

        this.exotic_worlds$customizeShapeButton = helper.addChild(
                Button.builder(exotic_worlds$CUSTOMIZE_LABEL, button -> exotic_worlds$openCustomizer()).build());

        WorldCreationUiState uiState = ((CreateWorldScreenAccessor) screen).exotic_worlds$uiState();
        uiState.addListener(this::exotic_worlds$followWorldType);
        exotic_worlds$followWorldType(uiState);
    }

    @Unique
    private void exotic_worlds$followWorldType(WorldCreationUiState uiState) {
        LevelStem overworld = uiState.getSettings().selectedDimensions().get(LevelStem.OVERWORLD).orElse(null);
        boolean takesShape = overworld != null;
        if (!takesShape && WorldShapes.selected() != WorldShapes.NORMAL) {
            WorldShapes.select(WorldShapes.NORMAL);
            this.exotic_worlds$shapeButton.setValue(WorldShapes.NORMAL);
        }

        this.exotic_worlds$terrainTilesAtSeam = !takesShape || ShapedChunkGenerator.tilesAtSeam(overworld.generator());
        this.exotic_worlds$shapeButton.active = takesShape;
        this.exotic_worlds$shapeButton.setTooltip(exotic_worlds$tooltipFor(WorldShapes.selected()));
        exotic_worlds$refreshCustomizeButton();
    }

    @Unique
    private Tooltip exotic_worlds$tooltipFor(WorldShape shape) {
        return Tooltip.create(this.exotic_worlds$terrainTilesAtSeam ? shape.hint() : exotic_worlds$SEAM_WARNING);
    }

    @Unique
    private void exotic_worlds$refreshCustomizeButton() {
        this.exotic_worlds$customizeShapeButton.active =
                this.exotic_worlds$shapeButton.active && ShapeCustomizers.of(WorldShapes.selected().id()) != null;
    }

    @Unique
    private static void exotic_worlds$openCustomizer() {
        ShapeCustomizers.Customizer customizer = ShapeCustomizers.of(WorldShapes.selected().id());
        if (customizer == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Screen parent = minecraft.gui.screen();
        if (parent != null) {
            minecraft.gui.setScreen(customizer.createScreen(parent));
        }
    }
}
