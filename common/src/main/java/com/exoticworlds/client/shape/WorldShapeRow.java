package com.exoticworlds.client.shape;

import java.util.List;
import java.util.function.UnaryOperator;

import com.ascendiacore.client.createworld.WorldTabRow;
import com.ascendiacore.gui.base.component.GuiComponent;
import com.ascendiacore.gui.theme.Themes;
import com.ascendiacore.gui.widget.button.Button;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.shape.WorldShapes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.dimension.LevelStem;

public final class WorldShapeRow implements WorldTabRow {
    private static final Component LABEL = Component.translatable("gui.exotic_worlds.world_shape_settings");
    private static final Component HINT = Component.translatable("gui.exotic_worlds.world_shape_settings.hint");
    private static final Component SEAM_WARNING =
            Component.translatable("gui.exotic_worlds.world_shape.seam_warning");

    private final UnaryOperator<Screen> settingsScreen;

    public WorldShapeRow(UnaryOperator<Screen> settingsScreen) {
        this.settingsScreen = settingsScreen;
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
        Minecraft minecraft = Minecraft.getInstance();
        Screen parent = minecraft.screen;
        if (parent != null) {
            minecraft.setScreen(this.settingsScreen.apply(parent));
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
