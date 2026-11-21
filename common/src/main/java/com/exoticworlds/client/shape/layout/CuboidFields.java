package com.exoticworlds.client.shape.layout;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.ascendiacore.gui.layout.Column;
import com.ascendiacore.gui.theme.Theme;
import com.ascendiacore.gui.widget.numberfield.NumberField;
import com.ascendiacore.gui.widget.numberfield.model.Bounds;
import com.ascendiacore.gui.widget.panel.model.FillStyle;
import com.exoticworlds.core.CuboidShape;
import com.exoticworlds.core.WorldLoopSizes;

final class CuboidFields {
    private final NumberField sizeA;
    private final NumberField sizeB;
    private final NumberField sizeC;
    private final Column section = new Column();

    CuboidFields(Theme theme, FillStyle fill) {
        Optional<Bounds> bounds = Optional.of(
                new Bounds(WorldLoopSizes.MIN_CHUNK_WIDTH, WorldLoopSizes.MAX_CUBOID_EDGE_CHUNKS));
        this.sizeA = LoopFields.sizeField(theme, fill, SettingsLines.CUBOID_SIZE_A, bounds);
        this.sizeB = LoopFields.sizeField(theme, fill, SettingsLines.CUBOID_SIZE_B, bounds);
        this.sizeC = LoopFields.sizeField(theme, fill, SettingsLines.CUBOID_SIZE_C, bounds);
        this.section.setGap(LoopFields.GAP);
        this.section.add(this.sizeA);
        this.section.add(this.sizeB);
        this.section.add(this.sizeC);
        this.show(null);
    }

    Column section() {
        return this.section;
    }

    void show(@Nullable CuboidShape shape) {
        this.sizeA.setValue(shape == null ? WorldLoopSizes.DEFAULT_CHUNK_WIDTH : shape.aChunks());
        this.sizeB.setValue(shape == null ? WorldLoopSizes.DEFAULT_CHUNK_WIDTH : shape.bChunks());
        this.sizeC.setValue(shape == null ? WorldLoopSizes.DEFAULT_CHUNK_WIDTH : shape.cChunks());
    }

    @Nullable CuboidShape shape() {
        Integer aChunks = this.sizeA.value();
        Integer bChunks = this.sizeB.value();
        Integer cChunks = this.sizeC.value();
        return aChunks == null || bChunks == null || cChunks == null
                ? null
                : new CuboidShape(aChunks, bChunks, cChunks);
    }
}
