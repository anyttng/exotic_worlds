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
    private static final Component toroidal$SHAPE_LABEL = Component.translatable("gui.exotic_worlds.world_shape");

    @Unique
    private static final Component toroidal$CUSTOMIZE_LABEL = Component.translatable("selectWorld.customizeType");

    @Unique
    private static final Component toroidal$SEAM_WARNING =
            Component.translatable("gui.exotic_worlds.world_shape.seam_warning");

    @Unique
    private static final int toroidal$SHAPE_BUTTON_WIDTH = 150;

    @Unique
    private static final int toroidal$SHAPE_BUTTON_HEIGHT = 20;

    @Unique
    private CycleButton<WorldShape> toroidal$shapeButton;

    @Unique
    private Button toroidal$customizeShapeButton;

    @Unique
    private boolean toroidal$terrainTilesAtSeam = true;

    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/CommonLayouts;labeledElement(Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/gui/layouts/LayoutElement;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/client/gui/layouts/Layout;",
                    ordinal = 0))
    private void toroidal$addWorldShapeRow(CallbackInfo ci, @Local GridLayout.RowHelper helper,
            @Local(argsOnly = true) CreateWorldScreen screen) {
        this.toroidal$shapeButton = helper.addChild(CycleButton.builder((WorldShape shape) -> shape.label())
                .withValues(WorldShapes.shapes())
                .withInitialValue(WorldShapes.selected())
                .withTooltip(this::toroidal$tooltipFor)
                .create(0, 0, toroidal$SHAPE_BUTTON_WIDTH, toroidal$SHAPE_BUTTON_HEIGHT, toroidal$SHAPE_LABEL,
                        (button, shape) -> {
                            WorldShapes.select(shape);
                            toroidal$refreshCustomizeButton();
                        }));

        this.toroidal$customizeShapeButton = helper.addChild(
                Button.builder(toroidal$CUSTOMIZE_LABEL, button -> toroidal$openCustomizer()).build());

        WorldCreationUiState uiState = ((CreateWorldScreenAccessor) screen).toroidal$uiState();
        uiState.addListener(this::toroidal$followWorldType);
        toroidal$followWorldType(uiState);
    }

    @Unique
    private void toroidal$followWorldType(WorldCreationUiState uiState) {
        LevelStem overworld = uiState.getSettings().selectedDimensions().get(LevelStem.OVERWORLD).orElse(null);
        boolean takesShape = overworld != null;
        if (!takesShape && WorldShapes.selected() != WorldShapes.NORMAL) {
            WorldShapes.select(WorldShapes.NORMAL);
            this.toroidal$shapeButton.setValue(WorldShapes.NORMAL);
        }

        this.toroidal$terrainTilesAtSeam = !takesShape || ShapedChunkGenerator.tilesAtSeam(overworld.generator());
        this.toroidal$shapeButton.active = takesShape;
        this.toroidal$shapeButton.setTooltip(toroidal$tooltipFor(WorldShapes.selected()));
        toroidal$refreshCustomizeButton();
    }

    @Unique
    private Tooltip toroidal$tooltipFor(WorldShape shape) {
        return Tooltip.create(this.toroidal$terrainTilesAtSeam ? shape.hint() : toroidal$SEAM_WARNING);
    }

    @Unique
    private void toroidal$refreshCustomizeButton() {
        this.toroidal$customizeShapeButton.active =
                this.toroidal$shapeButton.active && ShapeCustomizers.of(WorldShapes.selected().id()) != null;
    }

    @Unique
    private static void toroidal$openCustomizer() {
        ShapeCustomizers.Customizer customizer = ShapeCustomizers.of(WorldShapes.selected().id());
        if (customizer == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Screen parent = minecraft.screen;
        if (parent != null) {
            minecraft.setScreen(customizer.createScreen(parent));
        }
    }
}
