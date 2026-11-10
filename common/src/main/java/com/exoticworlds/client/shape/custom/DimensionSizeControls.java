package com.exoticworlds.client.shape.custom;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.client.shape.SpawnControls;
import com.exoticworlds.client.shape.LoopSizeControls;
import com.exoticworlds.client.shape.SizeControls;
import com.exoticworlds.client.shape.SizeField;
import com.exoticworlds.client.shape.cylinder.CylinderSettingsScreen;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.shape.custom.CustomStem;
import com.exoticworlds.shape.custom.StemKind;
import com.exoticworlds.shape.custom.VanillaDimension;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.core.Direction;

public final class DimensionSizeControls implements SizeControls {
    private static final String SIZE_X_LABEL_KEY = "gui.exotic_worlds.toroidal_settings.size_x";
    private static final String SIZE_Z_LABEL_KEY = "gui.exotic_worlds.toroidal_settings.size_z";
    private static final String INCOMPLETE = "Done waits for isComplete";

    private final CustomStem dimension;
    private final StemKind kind;
    private final Runnable onChange;

    private final SizeField first;
    private final @Nullable SizeField second;
    private final @Nullable SpawnControls spawn;
    private Direction.Axis axis;

    public DimensionSizeControls(CustomStem dimension, LoopSpans current, SpawnPoint spawn,
            Runnable onChange) {
        this.dimension = dimension;
        this.kind = Objects.requireNonNull(StemKind.of(current), "A custom dimension holds a stated kind");
        this.onChange = onChange;
        this.axis = current.loops(Direction.Axis.X) ? Direction.Axis.X : Direction.Axis.Z;
        if (this.kind == StemKind.TORUS) {
            this.first = this.sizeField(SIZE_X_LABEL_KEY, current.chunkWidth(Direction.Axis.X));
            this.second = this.sizeField(SIZE_Z_LABEL_KEY, current.chunkWidth(Direction.Axis.Z));
        } else {
            this.first = dimension == VanillaDimension.END
                    ? SizeField.end(current.chunkWidth(this.axis))
                    : SizeField.world(current.chunkWidth(this.axis));
            this.second = null;
        }

        this.spawn = dimension != VanillaDimension.OVERWORLD ? null
                : this.kind == StemKind.TORUS
                        ? SpawnControls.torus(spawn)
                        : SpawnControls.cylinder(spawn, this.axis);
    }

    @Override
    public void addFields(Font font, LinearLayout contents) {
        if (this.kind == StemKind.CYLINDER) {
            contents.addChild(CycleButton.builder(CylinderSettingsScreen::axisName)
                    .withValues(Direction.Axis.X, Direction.Axis.Z)
                    .withInitialValue(this.axis)
                    .withTooltip(chosen -> Tooltip.create(CylinderSettingsScreen.AXIS_HINT))
                    .create(0, 0, LoopSizeControls.FIELD_WIDTH, LoopSizeControls.FIELD_HEIGHT,
                            CylinderSettingsScreen.AXIS_LABEL, (button, chosen) -> this.chooseAxis(chosen)));
        }

        this.first.add(font, contents, this::refresh);
        if (this.second != null) {
            this.second.add(font, contents, this::refresh);
        }

        if (this.spawn != null) {
            this.spawn.add(font, contents, this::refresh);
        }
    }

    @Override
    public void refresh() {
        this.first.update();
        if (this.second != null) {
            this.second.update();
        }

        if (this.spawn != null) {
            Integer firstWidth = this.first.effective();
            this.spawn.update(firstWidth, this.second != null ? this.second.effective() : firstWidth);
        }

        this.onChange.run();
    }

    @Override
    public boolean isComplete() {
        return this.effectiveSpans() != null && (this.spawn == null || this.spawn.isComplete());
    }

    public @Nullable LoopSpans effectiveSpans() {
        Integer firstWidth = this.first.effective();
        if (firstWidth == null) {
            return null;
        }

        if (this.second == null) {
            return LoopSpans.ofWidth(this.axis, firstWidth);
        }

        Integer secondWidth = this.second.effective();
        return secondWidth == null ? null : LoopSpans.ofWidths(firstWidth, secondWidth);
    }

    public @Nullable SpawnPoint spawn() {
        return this.spawn == null ? null : Objects.requireNonNull(this.spawn.effective(), INCOMPLETE);
    }

    public LoopSpans spans() {
        return Objects.requireNonNull(this.effectiveSpans(), INCOMPLETE);
    }

    private void chooseAxis(Direction.Axis chosen) {
        this.axis = chosen;
        if (this.spawn != null) {
            this.spawn.loopedAxis(chosen);
        }

        this.refresh();
    }

    private SizeField sizeField(String labelKey, int chunkWidth) {
        return this.dimension == VanillaDimension.END
                ? SizeField.end(labelKey, chunkWidth)
                : SizeField.world(labelKey, chunkWidth);
    }
}
