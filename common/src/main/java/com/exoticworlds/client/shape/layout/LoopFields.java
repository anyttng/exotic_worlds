package com.exoticworlds.client.shape.layout;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.ascendiacore.gui.base.property.Value;
import com.ascendiacore.gui.layout.Column;
import com.ascendiacore.gui.layout.Row;
import com.ascendiacore.gui.layout.model.Length;
import com.ascendiacore.gui.theme.Theme;
import com.ascendiacore.gui.widget.cyclebutton.CycleButton;
import com.ascendiacore.gui.widget.numberfield.NumberField;
import com.ascendiacore.gui.widget.numberfield.model.Bounds;
import com.ascendiacore.gui.widget.panel.model.FillStyle;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.core.WorldLoopSizes;
import com.exoticworlds.shape.layout.ShapeKind;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

final class LoopFields {
    static final int GAP = 4;
    private static final int PAIR_GAP = 6;
    private static final int SIZE_MAX_LENGTH = 7;
    private static final String AXIS_HINT_X = "x";
    private static final String AXIS_HINT_Z = "z";
    private static final Optional<Bounds> SIZE_BOUNDS =
            Optional.of(new Bounds(WorldLoopSizes.MIN_CHUNK_WIDTH, WorldLoopSizes.MAX_CHUNK_WIDTH));

    private final NumberField sizeX;
    private final NumberField sizeZ;
    private final NumberField shift;
    private final NumberField size;
    private final Value<Direction.Axis> axis = Value.of(Direction.Axis.X);
    private final Column torus = new Column();
    private final Column cylinder = new Column();

    LoopFields(Theme theme, FillStyle fill, Runnable onChange) {
        this.sizeX = sizeField(theme, fill, SettingsLines.SIZE_X, onChange);
        this.sizeZ = sizeField(theme, fill, SettingsLines.SIZE_Z, onChange);
        this.size = sizeField(theme, fill, SettingsLines.SIZE, onChange);
        this.shift = new NumberField(fill, theme.textField(), SettingsLines.SHIFT, this::shiftBounds, onChange);
        this.shift.setMaxLength(SIZE_MAX_LENGTH);
        this.shift.setValueLine(this::shiftLaps);
        this.fillTorus();
        this.fillCylinder(theme, onChange);
        this.show(null);
    }

    Column torus() {
        return this.torus;
    }

    Column cylinder() {
        return this.cylinder;
    }

    void show(@Nullable LoopSpans spans) {
        ShapeKind kind = ShapeKind.of(spans);
        if (spans == null || kind == ShapeKind.NORMAL) {
            this.showWidths(WorldLoopSizes.DEFAULT_CHUNK_WIDTH, WorldLoopSizes.DEFAULT_CHUNK_WIDTH, 0);
            return;
        }

        if (kind == ShapeKind.CYLINDER) {
            this.axis.set(spans.loops(Direction.Axis.X) ? Direction.Axis.X : Direction.Axis.Z);
            int width = spans.chunkWidth(this.axis.get());
            this.showWidths(width, width, 0);
            return;
        }

        int xChunkWidth = spans.chunkWidth(Direction.Axis.X);
        this.showWidths(xChunkWidth, spans.chunkWidth(Direction.Axis.Z), Math.floorMod(spans.skewChunks(), xChunkWidth));
    }

    @Nullable LoopSpans spans(ShapeKind kind) {
        if (kind == ShapeKind.CYLINDER) {
            Integer width = this.size.value();
            return width == null ? null : LoopSpans.ofWidth(this.axis.get(), width);
        }

        Integer xChunkWidth = this.sizeX.value();
        Integer zChunkWidth = this.sizeZ.value();
        Integer shiftChunks = this.shift.value();
        return kind != ShapeKind.TORUS || xChunkWidth == null || zChunkWidth == null || shiftChunks == null
                ? null
                : LoopSpans.ofWidths(xChunkWidth, zChunkWidth).withSkew(shiftChunks);
    }

    void carryWidth(ShapeKind from, ShapeKind to) {
        if (from == ShapeKind.CYLINDER && to == ShapeKind.TORUS) {
            Integer width = this.size.value();
            if (width != null) {
                this.sizeX.setValue(width);
                this.sizeZ.setValue(width);
            }
        } else if (from == ShapeKind.TORUS && to == ShapeKind.CYLINDER) {
            Integer width = this.sizeX.value();
            if (width != null) {
                this.size.setValue(width);
            }
        }
    }

    private void showWidths(int xChunkWidth, int zChunkWidth, int shiftChunks) {
        this.sizeX.setValue(xChunkWidth);
        this.sizeZ.setValue(zChunkWidth);
        this.size.setValue(xChunkWidth);
        this.shift.setValue(shiftChunks);
    }

    private void fillTorus() {
        Row sizes = new Row();
        sizes.setGap(PAIR_GAP);
        sizes.add(this.sizeX, Length.fill());
        sizes.add(this.sizeZ, Length.fill());
        this.torus.setGap(GAP);
        this.torus.add(sizes);
        this.torus.add(this.shift);
    }

    private void fillCylinder(Theme theme, Runnable onChange) {
        CycleButton<Direction.Axis> axisButton = new CycleButton<>(theme.button(),
                Component.translatable(SettingsLines.AXIS_KEY), () -> List.of(Direction.Axis.X, Direction.Axis.Z),
                this.axis, SettingsLines::axisName);
        axisButton.setHint(theme.tooltip(), SettingsLines.AXIS_KEY,
                picked -> picked == Direction.Axis.X ? AXIS_HINT_X : AXIS_HINT_Z);
        axisButton.setOnChange(picked -> onChange.run());
        this.cylinder.setGap(GAP);
        this.cylinder.add(axisButton);
        this.cylinder.add(this.size);
    }

    private Optional<Bounds> shiftBounds() {
        Integer xChunkWidth = this.sizeX.value();
        return xChunkWidth == null ? Optional.empty() : Optional.of(new Bounds(0, xChunkWidth - 1));
    }

    private Component shiftLaps(int shiftChunks) {
        Integer xChunkWidth = this.sizeX.value();
        return xChunkWidth == null ? Component.empty() : SettingsLines.shiftLaps(xChunkWidth, shiftChunks);
    }

    private static NumberField sizeField(Theme theme, FillStyle fill, Component caption, Runnable onChange) {
        NumberField field = new NumberField(fill, theme.textField(), caption, () -> SIZE_BOUNDS, onChange);
        field.setMaxLength(SIZE_MAX_LENGTH);
        field.setValueLine(SettingsLines::sizeEffective);
        return field;
    }
}
