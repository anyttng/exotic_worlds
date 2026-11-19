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
import com.exoticworlds.api.v2.shape.BuiltInShapes;
import com.exoticworlds.core.NetherScales;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.core.WorldLoopSizes;
import com.exoticworlds.shape.layout.DimensionLayout;
import com.exoticworlds.shape.layout.OverworldDerivation;
import com.exoticworlds.shape.layout.ShapeKind;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

final class DimensionPage {
    private final DimensionOffer offer;
    private final Supplier<@Nullable LoopSpans> overworldSpans;
    private final Value<ShapeKind> kind = Value.of(ShapeKind.NORMAL);
    private final Value<Integer> netherScale = Value.of(NetherScales.DEFAULT);
    private final LoopFields loops;
    private final SpawnFields spawn;
    private final OptionControls declared;
    private final Column page = new Column();
    private GenerationOptions options;
    private ShapeKind shownKind = ShapeKind.NORMAL;

    DimensionPage(DimensionOffer offer, Theme theme, FillStyle fill, Supplier<@Nullable LoopSpans> overworldSpans,
            DimensionLayout shown, @Nullable Component lockReason) {
        this.offer = offer;
        this.overworldSpans = overworldSpans;
        this.options = shown.options();
        this.loops = new LoopFields(theme, fill, offer.minChunkWidth(), offer.defaultChunkWidth());
        this.spawn = new SpawnFields(theme, fill, this::spawnChunkWidth);
        this.declared = new OptionControls(theme, fill, offer.dimension(), this::resolvedShape, this::resolvedSpans);
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
        this.declared.show(this.options);
    }

    void showNetherScale(int scale) {
        this.netherScale.set(scale);
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
        if (this.resolvedShape() == null) {
            return this.options;
        }

        SpawnPoint point = this.offer.spawnPoint() ? this.spawn.point() : this.options.get(SpawnPoint.OPTION);
        return point == null ? null : this.declared.commit(this.options.with(SpawnPoint.OPTION, point));
    }

    private void build(Theme theme, FillStyle fill, @Nullable Component lockReason) {
        CycleButton<ShapeKind> shape = new CycleButton<>(theme.button(), Component.translatable(SettingsLines.SHAPE_KEY),
                this.offer::kinds, this.kind, SettingsLines::shapeName);
        shape.setHint(theme.tooltip(), SettingsLines.SHAPE_KEY, picked -> picked.name().toLowerCase(Locale.ROOT));
        shape.setOnChange(this::changeKind);

        Column additional = new Column();
        additional.setGap(LoopFields.GAP);
        additional.add(Heading.of(fill, SettingsLines.ADDITIONAL));
        additional.add(this.declared.section());

        this.loops.torus().setVisible(() -> this.kind.get() == ShapeKind.TORUS);
        this.loops.cylinder().setVisible(() -> this.kind.get() == ShapeKind.CYLINDER);
        this.spawn.section().setVisible(() -> this.offer.spawnPoint() && this.resolvedKind() != ShapeKind.NORMAL);
        additional.setVisible(this.declared::anyOffered);

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

    private @Nullable Identifier resolvedShape() {
        return switch (this.resolvedKind()) {
            case TORUS -> BuiltInShapes.TORUS;
            case CYLINDER -> BuiltInShapes.CYLINDER;
            case NORMAL, FROM_OVERWORLD -> null;
        };
    }

    private void changeKind(ShapeKind picked) {
        this.loops.carryWidth(this.shownKind, picked);
        this.shownKind = picked;
    }

    private @Nullable Integer spawnChunkWidth(Direction.Axis axis) {
        LoopSpans spans = this.loops.spans(this.kind.get());
        if (spans == null) {
            return null;
        }

        return spans.loops(axis) ? spans.chunkWidth(axis) : WorldLoopSizes.ENDLESS_AXIS_WIDTH;
    }
}
