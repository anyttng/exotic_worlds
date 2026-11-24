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
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.WorldLoopSizes;
import com.exoticworlds.shape.cylinder.Cylinder;
import com.exoticworlds.shape.layout.ShapeKind;
import com.exoticworlds.shape.torus.Torus;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

final class LoopFields {
    static final int GAP_PIXELS = 4;
    private static final int PAIR_GAP_PIXELS = 6;
    private static final int SIZE_MAX_LENGTH = 7;
    private static final String AXIS_HINT_X = "x";
    private static final String AXIS_HINT_Z = "z";

    private final NumberField sizeX;
    private final NumberField sizeZ;
    private final NumberField shift;
    private final NumberField size;
    private final Value<Direction.Axis> axis = Value.of(Direction.Axis.X);
    private final Column torus = new Column();
    private final Column cylinder = new Column();
    private final int defaultWidthChunks;

    LoopFields(Theme theme, FillStyle fill, int minWidthChunks, int defaultWidthChunks) {
        Optional<Bounds> bounds = Optional.of(new Bounds(minWidthChunks, WorldLoopSizes.MAX_WIDTH_CHUNKS));
        this.defaultWidthChunks = defaultWidthChunks;
        this.sizeX = sizeField(theme, fill, SettingsLines.sizeCaption(Direction.Axis.X, minWidthChunks), bounds);
        this.sizeZ = sizeField(theme, fill, SettingsLines.sizeCaption(Direction.Axis.Z, minWidthChunks), bounds);
        this.size = sizeField(theme, fill, SettingsLines.sizeCaption(minWidthChunks), bounds);
        this.shift = new NumberField(fill, theme.textField(), SettingsLines.SHIFT, this::shiftBounds, () -> {
        });
        this.shift.setMaxLength(SIZE_MAX_LENGTH);
        this.shift.setValueLine(this::shiftLaps);
        this.fillTorus();
        this.fillCylinder(theme);
        this.show(null);
    }

    Column getTorus() {
        return this.torus;
    }

    Column getCylinder() {
        return this.cylinder;
    }

    void show(@Nullable FlatShape spans) {
        ShapeKind kind = ShapeKind.of(spans);
        if (spans == null || kind == ShapeKind.NORMAL) {
            this.showWidths(this.defaultWidthChunks, this.defaultWidthChunks, 0);
            return;
        }

        if (kind == ShapeKind.CYLINDER) {
            ShapeOptions size = Cylinder.SHAPE.optionsFor(spans);
            int widthChunks = size.get(Cylinder.WIDTH);
            this.axis.set(size.get(Cylinder.AXIS));
            this.showWidths(widthChunks, widthChunks, 0);
            return;
        }

        ShapeOptions size = Torus.SHAPE.optionsFor(spans);
        this.showWidths(size.get(Torus.X_WIDTH), size.get(Torus.Z_WIDTH), size.get(Torus.SHIFT));
    }

    @Nullable ShapeOptions size(ShapeKind kind) {
        if (kind == ShapeKind.CYLINDER) {
            Integer widthChunks = this.size.value();
            return widthChunks == null ? null
                    : ShapeOptions.DEFAULT.with(Cylinder.AXIS, this.axis.get()).with(Cylinder.WIDTH, widthChunks);
        }

        Integer xWidthChunks = this.sizeX.value();
        Integer zWidthChunks = this.sizeZ.value();
        Integer shiftChunks = this.shift.value();
        return kind != ShapeKind.TORUS || xWidthChunks == null || zWidthChunks == null || shiftChunks == null
                ? null
                : ShapeOptions.DEFAULT.with(Torus.X_WIDTH, xWidthChunks).with(Torus.Z_WIDTH, zWidthChunks)
                        .with(Torus.SHIFT, shiftChunks);
    }

    void carryWidth(ShapeKind from, ShapeKind to) {
        if (from == ShapeKind.CYLINDER && to == ShapeKind.TORUS) {
            Integer widthChunks = this.size.value();
            if (widthChunks != null) {
                this.sizeX.setValue(widthChunks);
                this.sizeZ.setValue(widthChunks);
            }
        } else if (from == ShapeKind.TORUS && to == ShapeKind.CYLINDER) {
            Integer widthChunks = this.sizeX.value();
            if (widthChunks != null) {
                this.size.setValue(widthChunks);
            }
        }
    }

    private void showWidths(int xWidthChunks, int zWidthChunks, int shiftChunks) {
        this.sizeX.setValue(xWidthChunks);
        this.sizeZ.setValue(zWidthChunks);
        this.size.setValue(xWidthChunks);
        this.shift.setValue(shiftChunks);
    }

    private void fillTorus() {
        Row sizes = new Row();
        sizes.setGap(PAIR_GAP_PIXELS);
        sizes.add(this.sizeX, Length.fill());
        sizes.add(this.sizeZ, Length.fill());
        this.torus.setGap(GAP_PIXELS);
        this.torus.add(sizes);
        this.torus.add(this.shift);
    }

    private void fillCylinder(Theme theme) {
        CycleButton<Direction.Axis> axisButton = new CycleButton<>(theme.button(),
                Component.translatable(SettingsLines.AXIS_KEY), () -> List.of(Direction.Axis.X, Direction.Axis.Z),
                this.axis, SettingsLines::axisName);
        axisButton.setHint(theme.tooltip(), SettingsLines.AXIS_KEY,
                picked -> picked == Direction.Axis.X ? AXIS_HINT_X : AXIS_HINT_Z);
        this.cylinder.setGap(GAP_PIXELS);
        this.cylinder.add(axisButton);
        this.cylinder.add(this.size);
    }

    private Optional<Bounds> shiftBounds() {
        Integer xWidthChunks = this.sizeX.value();
        return xWidthChunks == null ? Optional.empty() : Optional.of(new Bounds(0, xWidthChunks - 1));
    }

    private Component shiftLaps(int shiftChunks) {
        Integer xWidthChunks = this.sizeX.value();
        return xWidthChunks == null ? Component.empty() : SettingsLines.shiftLaps(xWidthChunks, shiftChunks);
    }

    private static NumberField sizeField(Theme theme, FillStyle fill, Component caption, Optional<Bounds> bounds) {
        NumberField field = new NumberField(fill, theme.textField(), caption, () -> bounds, () -> {
        });
        field.setMaxLength(SIZE_MAX_LENGTH);
        field.setValueLine(SettingsLines::sizeEffective);
        return field;
    }
}
