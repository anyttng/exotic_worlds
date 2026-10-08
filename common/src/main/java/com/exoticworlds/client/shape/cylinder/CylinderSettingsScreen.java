package com.exoticworlds.client.shape.cylinder;

import java.util.Locale;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.client.shape.LoopSettingsScreen;
import com.exoticworlds.client.shape.LoopSizeControls;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.shape.cylinder.CylinderSettings;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

public class CylinderSettingsScreen extends LoopSettingsScreen<CylinderSettings> {
    private static final Component TITLE = Component.translatable("gui.exotic_worlds.cylinder_settings.title");
    private static final Component AXIS_LABEL = Component.translatable("gui.exotic_worlds.cylinder_settings.axis");
    private static final Component AXIS_HINT = Component.translatable("gui.exotic_worlds.cylinder_settings.axis_hint");

    private Direction.Axis axis;

    public CylinderSettingsScreen(Screen parent, CylinderSettings current, Consumer<CylinderSettings> onDone) {
        super(TITLE, parent, onChange -> LoopSizeControls.single(current.chunkWidth(), current.netherScale(),
                current.endChunkWidth(), onChange), current.generationOptions(), CylinderSettings.OFFERED_OPTIONS,
                onDone);
        this.axis = current.axis();
    }

    @Override
    protected void addBeforeFields(Font font, LinearLayout contents) {
        contents.addChild(CycleButton.builder(CylinderSettingsScreen::axisName)
                .withValues(Direction.Axis.X, Direction.Axis.Z)
                .withInitialValue(this.axis)
                .withTooltip(chosen -> Tooltip.create(AXIS_HINT))
                .create(0, 0, LoopSizeControls.FIELD_WIDTH, LoopSizeControls.FIELD_HEIGHT, AXIS_LABEL,
                        (button, chosen) -> this.chooseAxis(chosen)));
    }

    @Override
    protected CylinderSettings build() {
        return new CylinderSettings(
                LoopSpans.ofWidth(this.axis, this.controls.effectiveSize(this.axis)),
                this.controls.netherScale(),
                LoopSpans.ofWidth(this.axis, this.controls.effectiveEndSize()),
                this.committedOptions());
    }

    @Override
    protected OptionContext newOptionContext() {
        return new ScreenContext();
    }

    private void chooseAxis(Direction.Axis chosen) {
        this.axis = chosen;
        this.onControlsChanged();
    }

    private static Component axisName(Direction.Axis axis) {
        return Component.literal(axis.getName().toUpperCase(Locale.ROOT));
    }

    private final class ScreenContext extends OptionContext {
        @Override
        public @Nullable Integer loopChunkWidth() {
            return CylinderSettingsScreen.this.controls.effectiveSize(CylinderSettingsScreen.this.axis);
        }

        @Override
        public @Nullable LoopSpans loopSpans() {
            Integer chunkWidth = this.loopChunkWidth();
            return chunkWidth == null ? null : LoopSpans.ofWidth(CylinderSettingsScreen.this.axis, chunkWidth);
        }
    }
}
