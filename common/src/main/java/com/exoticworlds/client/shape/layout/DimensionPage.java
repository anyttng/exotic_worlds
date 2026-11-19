package com.exoticworlds.client.shape.layout;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.ascendiacore.gui.base.property.Value;
import com.ascendiacore.gui.layout.Column;
import com.ascendiacore.gui.theme.Theme;
import com.ascendiacore.gui.widget.cyclebutton.CycleButton;
import com.ascendiacore.gui.widget.field.Field;
import com.ascendiacore.gui.widget.heading.Heading;
import com.ascendiacore.gui.widget.label.Label;
import com.ascendiacore.gui.widget.label.model.LabelRole;
import com.ascendiacore.gui.widget.panel.model.FillStyle;
import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.client.shape.climate.CompactBiomesFields;
import com.exoticworlds.core.NetherScales;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.core.WorldLoopSizes;
import com.exoticworlds.shape.climate.ClimateScale;
import com.exoticworlds.shape.climate.CompactBiomes;
import com.exoticworlds.shape.layout.DimensionLayout;
import com.exoticworlds.shape.layout.OverworldDerivation;
import com.exoticworlds.shape.layout.ShapeKind;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

final class DimensionPage {
    private final DimensionOffer offer;
    private final Supplier<@Nullable LoopSpans> overworldSpans;
    private final Value<ShapeKind> kind = Value.of(ShapeKind.NORMAL);
    private final Value<Boolean> torusOption = Value.of(false);
    private final Value<Integer> netherScale = Value.of(NetherScales.DEFAULT);
    private final LoopFields loops;
    private final SpawnFields spawn;
    private final CompactBiomesFields compactBiomes;
    private final Column page = new Column();
    private GenerationOptions options;
    private ShapeKind shownKind = ShapeKind.NORMAL;

    DimensionPage(DimensionOffer offer, Theme theme, FillStyle fill, @Nullable Screen parent,
            Supplier<@Nullable LoopSpans> overworldSpans, DimensionLayout shown, @Nullable Component lockReason) {
        this.offer = offer;
        this.overworldSpans = overworldSpans;
        this.options = shown.options();
        this.loops = new LoopFields(theme, fill, offer.minChunkWidth(), offer.defaultChunkWidth(), this::refresh);
        this.spawn = new SpawnFields(theme, fill, this::spawnChunkWidth, this::refresh);
        this.compactBiomes = new CompactBiomesFields(theme, fill, parent, this::refresh);
        this.build(theme, fill, lockReason);
        this.show(shown);
    }

    Column page() {
        return this.page;
    }

    Component title() {
        return this.offer.title();
    }

    boolean derivationKnown() {
        return this.offer.derivation() != null;
    }

    void show(DimensionLayout layout) {
        this.options = layout.options();
        this.kind.set(layout.kind());
        this.shownKind = layout.kind();
        this.loops.show(layout.spans());
        this.spawn.show(this.options.get(SpawnPoint.OPTION));
        this.compactBiomes.show(this.options.get(CompactBiomes.OPTION));
        DimensionOffer.TorusToggle toggle = this.offer.torusToggle();
        this.torusOption.set(toggle != null && this.options.get(toggle.option()));
        this.refresh();
    }

    void showNetherScale(int scale) {
        this.netherScale.set(scale);
        this.refresh();
    }

    int netherScale() {
        return NetherScales.normalize(this.netherScale.get(), this.netherScales());
    }

    @Nullable DimensionLayout layout() {
        ShapeKind chosen = this.kind.get();
        if (chosen == ShapeKind.NORMAL) {
            return DimensionLayout.normal(this.options);
        }

        GenerationOptions chosenOptions = this.chosenOptions();
        if (!this.page.fieldsValid() || chosenOptions == null) {
            return null;
        }

        if (chosen == ShapeKind.FROM_OVERWORLD) {
            return DimensionLayout.fromOverworld(chosenOptions);
        }

        LoopSpans spans = this.loops.spans(chosen);
        return spans == null ? null : DimensionLayout.looped(spans, chosenOptions);
    }

    private @Nullable GenerationOptions chosenOptions() {
        ShapeKind resolved = ShapeKind.of(this.resolvedSpans());
        if (resolved == ShapeKind.NORMAL) {
            return this.options;
        }

        ClimateScale scale = this.compactBiomes.scale();
        SpawnPoint point = this.offer.spawnPoint() ? this.spawn.point() : this.options.get(SpawnPoint.OPTION);
        if (scale == null || point == null) {
            return null;
        }

        GenerationOptions chosen = this.options.with(CompactBiomes.OPTION, scale).with(SpawnPoint.OPTION, point);
        DimensionOffer.TorusToggle toggle = this.offer.torusToggle();
        return toggle != null && resolved == ShapeKind.TORUS
                ? chosen.with(toggle.option(), this.torusOption.get())
                : chosen;
    }

    private void build(Theme theme, FillStyle fill, @Nullable Component lockReason) {
        CycleButton<ShapeKind> shape = new CycleButton<>(theme.button(), Component.translatable(SettingsLines.SHAPE_KEY),
                this.offer::kinds, this.kind, SettingsLines::shapeName);
        shape.setHint(theme.tooltip(), SettingsLines.SHAPE_KEY, picked -> picked.name().toLowerCase(Locale.ROOT));
        shape.setOnChange(this::changeKind);

        Column additional = new Column();
        additional.setGap(LoopFields.GAP);
        additional.add(Heading.of(fill, SettingsLines.ADDITIONAL));
        additional.add(this.compactBiomes.section());
        DimensionOffer.TorusToggle toggle = this.offer.torusToggle();
        if (toggle != null) {
            CycleButton<Boolean> torusToggle = OptionFields.toggle(theme, toggle.captionKey(), this.torusOption);
            torusToggle.setVisible(() -> this.resolvedKind() == ShapeKind.TORUS);
            additional.add(torusToggle);
        }

        this.loops.torus().setVisible(() -> this.kind.get() == ShapeKind.TORUS);
        this.loops.cylinder().setVisible(() -> this.kind.get() == ShapeKind.CYLINDER);
        this.spawn.section().setVisible(() -> this.offer.spawnPoint() && this.resolvedKind() != ShapeKind.NORMAL);
        additional.setVisible(() -> this.resolvedKind() != ShapeKind.NORMAL);

        this.page.setGap(LoopFields.GAP);
        this.page.add(Heading.of(fill, this.offer.title()));
        if (lockReason != null) {
            this.page.add(new Label(fill, LabelRole.MUTED, lockReason));
            this.page.setEnabled(false);
        }

        this.page.add(shape);
        this.page.add(this.netherScaleField(theme, fill));
        this.page.add(this.derivedLine(fill));
        this.page.add(this.loops.torus());
        this.page.add(this.loops.cylinder());
        this.page.add(this.spawn.section());
        this.page.add(additional);
    }

    private Field netherScaleField(Theme theme, FillStyle fill) {
        CycleButton<Integer> scale = new CycleButton<>(theme.button(), SettingsLines.NETHER_SCALE, this::netherScales,
                this.netherScale, picked -> SettingsLines.netherScaleName(this.netherScale()));
        scale.showValueOnly();
        scale.setOnChange(picked -> this.refresh());
        Field field = new Field(fill, SettingsLines.NETHER_SCALE, scale);
        field.setValueLine(() -> SettingsLines.shapeSummary(this.resolvedSpans()));
        field.setVisible(() -> this.offer.netherScale() && this.kind.get() == ShapeKind.FROM_OVERWORLD);
        return field;
    }

    private Label derivedLine(FillStyle fill) {
        Label line = new Label(fill, LabelRole.MUTED, Component.empty());
        line.setText(() -> SettingsLines.shapeSummary(this.resolvedSpans()));
        line.setVisible(() -> this.derivationKnown() && !this.offer.netherScale()
                && this.kind.get() == ShapeKind.FROM_OVERWORLD);
        return line;
    }

    private List<Integer> netherScales() {
        LoopSpans overworld = this.overworldSpans.get();
        return overworld == null || ShapeKind.of(overworld) == ShapeKind.NORMAL
                ? List.of(this.netherScale.get())
                : OverworldDerivation.netherScales(overworld);
    }

    private @Nullable LoopSpans resolvedSpans() {
        ShapeKind chosen = this.kind.get();
        if (chosen != ShapeKind.FROM_OVERWORLD) {
            return this.loops.spans(chosen);
        }

        var derivation = this.offer.derivation();
        return derivation == null ? null : derivation.apply(this.overworldSpans.get(), this.netherScale.get());
    }

    private ShapeKind resolvedKind() {
        ShapeKind chosen = this.kind.get();
        return chosen == ShapeKind.FROM_OVERWORLD ? ShapeKind.of(this.resolvedSpans()) : chosen;
    }

    private void changeKind(ShapeKind picked) {
        this.loops.carryWidth(this.shownKind, picked);
        this.shownKind = picked;
        this.refresh();
    }

    private void refresh() {
        this.compactBiomes.preview(this.resolvedSpans(), this.options);
    }

    private @Nullable Integer spawnChunkWidth(Direction.Axis axis) {
        LoopSpans spans = this.loops.spans(this.kind.get());
        if (spans == null) {
            return null;
        }

        return spans.loops(axis) ? spans.chunkWidth(axis) : WorldLoopSizes.ENDLESS_AXIS_WIDTH;
    }
}
