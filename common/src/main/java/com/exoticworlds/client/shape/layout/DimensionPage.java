package com.exoticworlds.client.shape.layout;

import java.util.ArrayList;
import java.util.List;
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
import com.exoticworlds.api.v2.shape.DimensionContext;
import com.exoticworlds.api.v2.shape.DimensionShape;
import com.exoticworlds.core.CuboidShape;
import com.exoticworlds.core.NetherScales;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.core.WorldLoopSizes;
import com.exoticworlds.shape.WorldShape;
import com.exoticworlds.shape.layout.DeclaredShapes;
import com.exoticworlds.shape.layout.DimensionLayout;
import com.exoticworlds.shape.layout.OverworldDerivation;
import com.exoticworlds.shape.layout.ShapeKind;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.dimension.LevelStem;

final class DimensionPage {
    private final DimensionOffer offer;
    private final CoordinateScales scales;
    private final Supplier<DimensionLayout> overworld;
    private final Value<ShapeChoice> chosen = Value.of(ShapeChoice.of(ShapeKind.NORMAL));
    private final Value<Integer> netherScale = Value.of(NetherScales.DEFAULT);
    private final LoopFields loops;
    private final CuboidFields cuboid;
    private final SpawnFields spawn;
    private final OptionControls declared;
    private final Column page = new Column();
    private GenerationOptions options;
    private ShapeKind shownKind = ShapeKind.NORMAL;

    DimensionPage(DimensionOffer offer, Theme theme, FillStyle fill, CoordinateScales scales,
            Supplier<DimensionLayout> overworld, DimensionLayout shown, @Nullable Component lockReason) {
        this.offer = offer;
        this.scales = scales;
        this.overworld = overworld;
        this.options = shown.options();
        this.loops = new LoopFields(theme, fill, offer.minChunkWidth(), offer.defaultChunkWidth());
        this.cuboid = new CuboidFields(theme, fill);
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
        Identifier declaredId = layout.shape();
        DimensionShape shape = declaredId == null ? null : DeclaredShapes.of(declaredId);
        this.chosen.set(shape != null ? ShapeChoice.of(shape) : ShapeChoice.of(layout.kind()));
        this.shownKind = layout.kind();
        this.loops.show(layout.spans());
        this.cuboid.show(layout.cuboid());
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
        ShapeKind own = this.own();
        if (own == ShapeKind.NORMAL) {
            return DimensionLayout.normal(this.options);
        }

        GenerationOptions chosenOptions = this.chosenOptions();
        if (!this.page.fieldsValid() || chosenOptions == null) {
            return null;
        }

        DimensionShape shape = this.chosen.get().declared();
        if (shape != null) {
            return this.sizeError().getString().isEmpty()
                    ? DimensionLayout.declared(shape.id(), this.declaredSpans(shape), chosenOptions)
                    : null;
        }

        if (own == ShapeKind.FROM_OVERWORLD) {
            return DimensionLayout.fromOverworld(chosenOptions);
        }

        if (own == ShapeKind.CUBOID) {
            CuboidShape sizes = this.cuboid.shape();
            return sizes == null ? null : DimensionLayout.cuboid(sizes);
        }

        LoopSpans spans = own == null ? null : this.loops.spans(own);
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
        CycleButton<ShapeChoice> shape = new CycleButton<>(theme.button(),
                Component.translatable(SettingsLines.SHAPE_KEY), this::choices, this.chosen, ShapeChoice::name);
        shape.setHint(theme.tooltip(), SettingsLines.SHAPE_KEY, ShapeChoice::hintKey);
        shape.setOnChange(this::changeShape);

        Column additional = new Column();
        additional.setGap(LoopFields.GAP);
        additional.add(Heading.of(fill, SettingsLines.ADDITIONAL));
        additional.add(this.declared.section());

        this.loops.torus().setVisible(() -> this.own() == ShapeKind.TORUS);
        this.loops.cylinder().setVisible(() -> this.own() == ShapeKind.CYLINDER);
        this.cuboid.section().setVisible(() -> this.own() == ShapeKind.CUBOID);
        this.spawn.section().setVisible(() -> this.offer.spawnPoint() && this.takesOptions(this.resolvedKind()));
        additional.setVisible(this.declared::anyOffered);

        this.page.setGap(LoopFields.GAP);
        this.page.add(Heading.of(fill, this.offer.title()));
        if (lockReason != null) {
            this.page.add(new Label(fill, LabelRole.MUTED, lockReason));
            this.page.setEnabled(false);
        }

        this.page.add(shape);
        this.page.add(this.declaredHint(fill));
        this.page.add(this.netherScaleField(theme, fill));
        this.page.add(this.derivedLine(fill));
        this.page.add(this.sizeErrorLine(fill));
        this.page.add(this.loops.torus());
        this.page.add(this.loops.cylinder());
        this.page.add(this.cuboid.section());
        this.page.add(this.spawn.section());
        this.page.add(additional);
    }

    private List<ShapeChoice> choices() {
        List<ShapeChoice> choices = new ArrayList<>();
        this.offer.kinds().forEach(kind -> choices.add(ShapeChoice.of(kind)));
        DeclaredShapes.all().forEach(shape -> choices.add(ShapeChoice.of(shape)));
        return choices;
    }

    private Label declaredHint(FillStyle fill) {
        Label hint = new Label(fill, LabelRole.MUTED, Component.empty());
        hint.setText(() -> {
            DimensionShape shape = this.chosen.get().declared();
            return shape == null ? Component.empty() : WorldShape.hint(shape.id());
        });
        hint.setVisible(() -> this.chosen.get().declared() != null);
        return hint;
    }

    private Field netherScaleField(Theme theme, FillStyle fill) {
        CycleButton<Integer> scale = new CycleButton<>(theme.button(), SettingsLines.NETHER_SCALE, this::netherScales,
                this.netherScale, picked -> SettingsLines.netherScaleName(this.netherScale()));
        scale.showValueOnly();
        Field field = new Field(fill, SettingsLines.NETHER_SCALE, scale);
        field.setValueLine(() -> SettingsLines.shapeSummary(this.resolvedSpans()));
        field.setVisible(() -> this.offer.netherScale() && this.own() == ShapeKind.FROM_OVERWORLD);
        return field;
    }

    private Label derivedLine(FillStyle fill) {
        Label line = new Label(fill, LabelRole.MUTED, Component.empty());
        line.setText(() -> SettingsLines.shapeSummary(this.resolvedSpans()));
        line.setVisible(() -> this.chosen.get().declared() != null
                || (this.derivationKnown() && !this.offer.netherScale() && this.own() == ShapeKind.FROM_OVERWORLD));
        return line;
    }

    private Label sizeErrorLine(FillStyle fill) {
        Label line = new Label(fill, LabelRole.ERROR, Component.empty());
        line.setText(this::sizeError);
        line.setVisible(() -> !this.sizeError().getString().isEmpty());
        return line;
    }

    private Component sizeError() {
        DimensionShape shape = this.chosen.get().declared();
        LoopSpans spans = shape == null ? null : this.declaredSpans(shape);
        if (spans == null) {
            return Component.empty();
        }

        for (Direction.Axis axis : List.of(Direction.Axis.X, Direction.Axis.Z)) {
            if (spans.loops(axis) && !this.allowed(spans.chunkWidth(axis))) {
                return SettingsLines.shapeSizeError(spans.chunkWidth(axis), this.offer.minChunkWidth());
            }
        }

        return Component.empty();
    }

    private boolean allowed(int chunkWidth) {
        return chunkWidth >= this.offer.minChunkWidth() && chunkWidth <= WorldLoopSizes.MAX_CHUNK_WIDTH;
    }

    private List<Integer> netherScales() {
        LoopSpans spans = this.overworld.get().spans();
        return spans == null ? List.of(this.netherScale.get()) : OverworldDerivation.netherScales(spans);
    }

    private @Nullable LoopSpans resolvedSpans() {
        ShapeChoice choice = this.chosen.get();
        DimensionShape shape = choice.declared();
        if (shape != null) {
            return this.declaredSpans(shape);
        }

        ShapeKind own = choice.own();
        if (own != ShapeKind.FROM_OVERWORLD) {
            return own == null ? null : this.loops.spans(own);
        }

        var derivation = this.offer.derivation();
        return derivation == null ? null : derivation.apply(this.overworld.get().spans(), this.netherScale.get());
    }

    private @Nullable LoopSpans declaredSpans(DimensionShape shape) {
        GenerationOptions chosenOptions = this.declared.commit(this.options);
        if (chosenOptions == null) {
            return null;
        }

        boolean overworldItself = this.offer.dimension() == LevelStem.OVERWORLD;
        return DeclaredShapes.resolve(shape, new DimensionContext(this.offer.dimension(), chosenOptions,
                overworldItself ? null : this.overworld.get().spans(), this.scales.of(LevelStem.OVERWORLD),
                this.scales.of(this.offer.dimension())));
    }

    private @Nullable ShapeKind own() {
        return this.chosen.get().own();
    }

    private ShapeKind resolvedKind() {
        ShapeKind own = this.own();
        return own == null || own == ShapeKind.FROM_OVERWORLD ? ShapeKind.of(this.resolvedSpans()) : own;
    }

    private @Nullable Identifier resolvedShape() {
        DimensionShape shape = this.chosen.get().declared();
        if (shape != null) {
            return shape.id();
        }

        ShapeKind resolved = this.resolvedKind();
        if (!this.takesOptions(resolved)) {
            return null;
        }

        Identifier fromOverworld = this.own() == ShapeKind.FROM_OVERWORLD ? this.overworld.get().shape() : null;
        if (fromOverworld != null) {
            return fromOverworld;
        }

        return resolved == ShapeKind.TORUS ? BuiltInShapes.TORUS : BuiltInShapes.CYLINDER;
    }

    private boolean takesOptions(ShapeKind resolved) {
        return resolved != ShapeKind.NORMAL && resolved != ShapeKind.CUBOID;
    }

    private void changeShape(ShapeChoice picked) {
        ShapeKind own = picked.own();
        if (own != null) {
            this.loops.carryWidth(this.shownKind, own);
            this.shownKind = own;
        }
    }

    private @Nullable Integer spawnChunkWidth(Direction.Axis axis) {
        ShapeChoice choice = this.chosen.get();
        DimensionShape shape = choice.declared();
        ShapeKind own = choice.own();
        LoopSpans spans = shape != null ? this.declaredSpans(shape) : own == null ? null : this.loops.spans(own);
        if (spans == null) {
            return null;
        }

        return spans.loops(axis) ? spans.chunkWidth(axis) : WorldLoopSizes.ENDLESS_AXIS_WIDTH;
    }
}
