package com.exoticworlds.client.shape;

import java.util.List;

import com.ascendiacore.client.createworld.WorldTabRow;
import com.ascendiacore.gui.base.component.GuiComponent;
import com.ascendiacore.gui.theme.Themes;
import com.ascendiacore.gui.widget.button.Button;
import com.exoticworlds.api.v1.client.ShapeCustomizers;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.shape.WorldShapes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.LevelStem;

public final class WorldShapeRow implements WorldTabRow {
    private static final Component LABEL = Component.translatable("gui.exotic_worlds.world_shape_settings");
    private static final Component HINT = Component.translatable("gui.exotic_worlds.world_shape_settings.hint");
    private static final Component SEAM_WARNING =
            Component.translatable("gui.exotic_worlds.world_shape.seam_warning");

    private final ResourceLocation shapeId;

    public WorldShapeRow(ResourceLocation shapeId) {
        this.shapeId = shapeId;
    }

    @Override
    public List<GuiComponent> build(WorldCreationUiState state) {
        WorldType worldType = new WorldType();
        state.addListener(worldType::follow);
        worldType.follow(state);

        Button button = Button.text(Themes.VANILLA.button(), LABEL, this::openSettings);
        button.setEnabled(worldType::takesShape);
        button.setTooltip(Themes.VANILLA.tooltip(), () -> worldType.tilesAtSeam() ? HINT : SEAM_WARNING);
        return List.of(button);
    }

    private void openSettings() {
        ShapeCustomizers.Customizer customizer = ShapeCustomizers.of(this.shapeId);
        Minecraft minecraft = Minecraft.getInstance();
        Screen parent = minecraft.screen;
        if (customizer != null && parent != null) {
            minecraft.setScreen(customizer.createScreen(parent));
        }
    }

    private static final class WorldType {
        private boolean takesShape;
        private boolean tilesAtSeam = true;

        private void follow(WorldCreationUiState state) {
            LevelStem overworld =state.getSettings().selectedDimensions().get(LevelStem.OVERWORLD).orElse(null);
            this.takesShape = overworld != null;
            if (overworld == null) {
                WorldShapes.select(WorldShapes.NORMAL);
                this.tilesAtSeam = true;
                return;
            }

            this.tilesAtSeam = ShapedChunkGenerator.tilesAtSeam(overworld.generator());
        }

        private boolean takesShape() {
            return this.takesShape;
        }

        private boolean tilesAtSeam() {
            return this.tilesAtSeam;
        }
    }
}
