package com.exoticworlds.client.shape.layout;

import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import com.ascendiacore.gui.base.property.Value;
import com.ascendiacore.gui.layout.Column;
import com.ascendiacore.gui.theme.Theme;
import com.ascendiacore.gui.widget.cyclebutton.CycleButton;
import com.ascendiacore.gui.widget.heading.Heading;
import com.ascendiacore.gui.widget.panel.model.FillStyle;
import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.client.shape.climate.CompactBiomesFields;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.core.WorldLoopSizes;
import com.exoticworlds.shape.climate.ClimateScale;
import com.exoticworlds.shape.climate.CompactBiomes;
import com.exoticworlds.shape.layout.DimensionLayout;
import com.exoticworlds.shape.layout.ShapeKind;
import com.exoticworlds.shape.torus.GuaranteedLand;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

final class OverworldPage {
    private static final List<ShapeKind> KINDS = List.of(ShapeKind.NORMAL, ShapeKind.TORUS, ShapeKind.CYLINDER);

    private final Value<ShapeKind> kind = Value.of(ShapeKind.NORMAL);
    private final Value<Boolean> guaranteedLand = Value.of(false);
    private final LoopFields loops;
    private final SpawnFields spawn;
    private final CompactBiomesFields compactBiomes;
    private final Column page = new Column();
    private GenerationOptions options;
    private ShapeKind shownKind = ShapeKind.NORMAL;

    OverworldPage(Theme theme, FillStyle fill, Screen parent, DimensionLayout shown) {
        this.options = shown.options();
        this.loops = new LoopFields(theme, fill, this::refresh);
        this.spawn = new SpawnFields(theme, fill, this::spawnChunkWidth, this::refresh);
        this.compactBiomes = new CompactBiomesFields(theme, fill, parent, this::refresh);
        this.build(theme, fill);
        this.show(shown);
    }

    Column page() {
        return this.page;
    }

    void show(DimensionLayout layout) {
        this.options = layout.options();
        this.kind.set(layout.kind());
        this.shownKind = layout.kind();
        this.loops.show(layout.spans());
        this.spawn.show(this.options.get(SpawnPoint.OPTION));
        this.compactBiomes.show(this.options.get(CompactBiomes.OPTION));
        this.guaranteedLand.set(this.options.get(GuaranteedLand.OPTION));
        this.refresh();
    }

    @Nullable DimensionLayout layout() {
        ShapeKind chosen = this.kind.get();
        if (chosen == ShapeKind.NORMAL) {
            return DimensionLayout.normal(this.options);
        }

        LoopSpans spans = this.loops.spans(chosen);
        SpawnPoint point = this.spawn.point();
        ClimateScale scale = this.compactBiomes.scale();
        if (!this.page.fieldsValid() || spans == null || point == null || scale == null) {
            return null;
        }

        GenerationOptions chosenOptions = this.options.with(SpawnPoint.OPTION, point).with(CompactBiomes.OPTION, scale);
        return DimensionLayout.looped(spans, chosen == ShapeKind.TORUS
                ? chosenOptions.with(GuaranteedLand.OPTION, this.guaranteedLand.get())
                : chosenOptions);
    }

    private void build(Theme theme, FillStyle fill) {
        CycleButton<ShapeKind> shape = new CycleButton<>(theme.button(), Component.translatable(SettingsLines.SHAPE_KEY),
                () -> KINDS, this.kind, SettingsLines::shapeName);
        shape.setHint(theme.tooltip(), SettingsLines.SHAPE_KEY, picked -> picked.name().toLowerCase(Locale.ROOT));
        shape.setOnChange(this::changeKind);

        Column additional = new Column();
        additional.setGap(LoopFields.GAP);
        additional.add(Heading.of(fill, SettingsLines.ADDITIONAL));
        additional.add(this.compactBiomes.section());
        CycleButton<Boolean> land = OptionFields.toggle(theme, SettingsLines.GUARANTEED_LAND_KEY, this.guaranteedLand);
        land.setVisible(() -> this.kind.get() == ShapeKind.TORUS);
        additional.add(land);

        this.loops.torus().setVisible(() -> this.kind.get() == ShapeKind.TORUS);
        this.loops.cylinder().setVisible(() -> this.kind.get() == ShapeKind.CYLINDER);
        this.spawn.section().setVisible(this::loops);
        additional.setVisible(this::loops);

        this.page.setGap(LoopFields.GAP);
        this.page.add(Heading.of(fill, SettingsLines.OVERWORLD));
        this.page.add(shape);
        this.page.add(this.loops.torus());
        this.page.add(this.loops.cylinder());
        this.page.add(this.spawn.section());
        this.page.add(additional);
    }

    private void changeKind(ShapeKind picked) {
        this.loops.carryWidth(this.shownKind, picked);
        this.shownKind = picked;
        this.refresh();
    }

    private boolean loops() {
        return this.kind.get() != ShapeKind.NORMAL;
    }

    private void refresh() {
        this.compactBiomes.preview(this.loops.spans(this.kind.get()), this.options);
    }

    private @Nullable Integer spawnChunkWidth(Direction.Axis axis) {
        LoopSpans spans = this.loops.spans(this.kind.get());
        if (spans == null) {
            return null;
        }

        return spans.loops(axis) ? spans.chunkWidth(axis) : WorldLoopSizes.ENDLESS_AXIS_WIDTH;
    }
}
