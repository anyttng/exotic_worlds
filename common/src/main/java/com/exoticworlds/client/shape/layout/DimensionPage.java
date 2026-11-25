package com.exoticworlds.client.shape.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.NetherScales;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.SpawnPoint;
import com.exoticworlds.core.world.WorldLoopSizes;
import com.exoticworlds.shape.WorldShape;
import com.exoticworlds.shape.dimension.DeclaredShapes;
import com.exoticworlds.shape.dimension.DimensionContext;
import com.exoticworlds.shape.dimension.DimensionShape;
import com.exoticworlds.shape.layout.DimensionLayout;
import com.exoticworlds.shape.layout.OverworldDerivation;
import com.exoticworlds.shape.layout.ShapeKind;
import com.exoticworlds.shape.option.DeclaredOptions;
import com.exoticworlds.shape.option.DimensionOption;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.dimension.LevelStem;

final class DimensionPage {
    static final int GAP_PIXELS = 4;

    private final DimensionOffer offer;
    private final CoordinateScales scales;
    private final Supplier<DimensionLayout> overworld;
    private final Value<ShapeChoice> chosen = Value.of(ShapeChoice.of(ShapeKind.NORMAL));
    private final Value<Integer> netherScale = Value.of(NetherScales.DEFAULT);
    private final OptionControls parameters;
    private final SpawnFields spawn;
    private final OptionControls declared;
    private final Column page = new Column();
    private ShapeOptions options;

    DimensionPage(DimensionOffer offer, Theme theme, FillStyle fill, CoordinateScales scales,
            Supplier<DimensionLayout> overworld, DimensionLayout shown, @Nullable Component lockReason) {
        this.offer = offer;
        this.scales = scales;
        this.overworld = overworld;
        this.options = shown.options();
        this.parameters = new OptionControls(theme, fill, offer.dimension(), getParameters(), this::getChosenShape,
                this::getResolvedSpans);
        this.spawn = new SpawnFields(theme, fill, this::spawnWidthChunks);
        this.declared = new OptionControls(theme, fill, offer.dimension(), DeclaredOptions.getAll(),
                this::getResolvedShape, this::getResolvedSpans);
        this.build(theme, fill, lockReason);
        this.show(shown);
    }

    Column getPage() {
        return this.page;
    }

    Component getTitle() {
        return this.offer.title();
    }

    boolean isDerivationKnown() {
        return this.offer.derivation() != null;
    }

    void show(DimensionLayout layout) {
        this.options = layout.options();
        Identifier declaredId = layout.shape();
        DimensionShape shape = declaredId == null ? null : DeclaredShapes.of(declaredId);
        this.chosen.set(shape != null ? ShapeChoice.of(shape) : ShapeChoice.of(layout.kind()));
        this.parameters.show(this.getShownParameters(layout));
        this.spawn.show(this.options.get(SpawnPoint.OPTION));
        this.declared.show(this.options);
    }

    void showNetherScale(int scale) {
        this.netherScale.set(scale);
    }

    int getNetherScale() {
        return NetherScales.normalize(this.netherScale.get(), this.getNetherScales());
    }

    @Nullable DimensionLayout layout() {
        ShapeKind own = this.getOwn();
        if (own == ShapeKind.NORMAL) {
            return DimensionLayout.normal(this.options);
        }

        ShapeOptions chosenOptions = this.getChosenOptions();
        if (!this.page.areFieldsValid() || chosenOptions == null) {
            return null;
        }

        DimensionShape shape = this.chosen.get().declared();
        if (shape != null) {
            return this.getSizeError().getString().isEmpty()
                    ? DimensionLayout.declared(shape.getId(), this.declaredSpans(shape), chosenOptions)
                    : null;
        }

        if (own == ShapeKind.FROM_OVERWORLD) {
            return DimensionLayout.fromOverworld(chosenOptions);
        }

        FlatShape spans = own == null ? null : this.ownSpans(own);
        return spans == null ? null : DimensionLayout.looped(spans, chosenOptions);
    }

    private @Nullable ShapeOptions getChosenOptions() {
        if (this.getResolvedShape() == null) {
            return this.options;
        }

        SpawnPoint point = this.offer.hasSpawnPoint() ? this.spawn.getPoint() : this.options.get(SpawnPoint.OPTION);
        return point == null ? null : this.declared.commit(this.options.with(SpawnPoint.OPTION, point));
    }

    private void build(Theme theme, FillStyle fill, @Nullable Component lockReason) {
        CycleButton<ShapeChoice> shape = new CycleButton<>(theme.button(),
                Component.translatable(SettingsLines.SHAPE_KEY), this::getChoices, this.chosen, ShapeChoice::getName);
        shape.setHint(theme.tooltip(), SettingsLines.SHAPE_KEY, ShapeChoice::getHintKey);

        Column additional = new Column();
        additional.setGapPixels(GAP_PIXELS);
        additional.add(Heading.of(fill, SettingsLines.ADDITIONAL));
        additional.add(this.declared.getSection());

        this.spawn.getSection().setVisible(() -> this.offer.hasSpawnPoint() && this.getResolvedKind() != ShapeKind.NORMAL);
        additional.setVisible(this.declared::isAnyOffered);

        this.page.setGapPixels(GAP_PIXELS);
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
        this.page.add(this.parameters.getSection());
        this.page.add(this.spawn.getSection());
        this.page.add(additional);
    }

    private List<ShapeChoice> getChoices() {
        List<ShapeChoice> choices = new ArrayList<>();
        this.offer.kinds().forEach(kind -> choices.add(ShapeChoice.of(kind)));
        DeclaredShapes.getAll().forEach(shape -> choices.add(ShapeChoice.of(shape)));
        return choices;
    }

    private Label declaredHint(FillStyle fill) {
        Label hint = new Label(fill, LabelRole.MUTED, Component.empty());
        hint.setText(() -> {
            DimensionShape shape = this.chosen.get().declared();
            return shape == null ? Component.empty() : WorldShape.hint(shape.getId());
        });
        hint.setVisible(() -> this.chosen.get().declared() != null);
        return hint;
    }

    private Field netherScaleField(Theme theme, FillStyle fill) {
        CycleButton<Integer> scale = new CycleButton<>(theme.button(), SettingsLines.NETHER_SCALE, this::getNetherScales,
                this.netherScale, picked -> SettingsLines.netherScaleName(this.getNetherScale()));
        scale.showValueOnly();
        Field field = new Field(fill, SettingsLines.NETHER_SCALE, scale);
        field.setValueLine(() -> SettingsLines.shapeSummary(this.getResolvedSpans()));
        field.setVisible(() -> this.offer.hasNetherScale() && this.getOwn() == ShapeKind.FROM_OVERWORLD);
        return field;
    }

    private Label derivedLine(FillStyle fill) {
        Label line = new Label(fill, LabelRole.MUTED, Component.empty());
        line.setText(() -> SettingsLines.shapeSummary(this.getResolvedSpans()));
        line.setVisible(() -> this.chosen.get().declared() != null
                || (this.isDerivationKnown() && !this.offer.hasNetherScale() && this.getOwn() == ShapeKind.FROM_OVERWORLD));
        return line;
    }

    private Label sizeErrorLine(FillStyle fill) {
        Label line = new Label(fill, LabelRole.ERROR, Component.empty());
        line.setText(this::getSizeError);
        line.setVisible(() -> !this.getSizeError().getString().isEmpty());
        return line;
    }

    private Component getSizeError() {
        DimensionShape shape = this.chosen.get().declared();
        FlatShape spans = shape == null ? null : this.declaredSpans(shape);
        if (spans == null) {
            return Component.empty();
        }

        for (Direction.Axis axis : List.of(Direction.Axis.X, Direction.Axis.Z)) {
            if (spans.bounds().isLooped(axis) && !this.isAllowed(spans.bounds().getWidthChunks(axis))) {
                return SettingsLines.shapeSizeError(spans.bounds().getWidthChunks(axis),
                        WorldLoopSizes.getMinWidthChunks(this.offer.dimension()));
            }
        }

        return Component.empty();
    }

    private boolean isAllowed(int widthChunks) {
        return widthChunks >= WorldLoopSizes.getMinWidthChunks(this.offer.dimension())
                && widthChunks <= WorldLoopSizes.MAX_WIDTH_CHUNKS;
    }

    private List<Integer> getNetherScales() {
        FlatShape spans = this.overworld.get().spans();
        return spans == null ? List.of(this.netherScale.get()) : OverworldDerivation.netherScales(spans);
    }

    private @Nullable FlatShape getResolvedSpans() {
        ShapeChoice choice = this.chosen.get();
        DimensionShape shape = choice.declared();
        if (shape != null) {
            return this.declaredSpans(shape);
        }

        ShapeKind own = choice.own();
        if (own != ShapeKind.FROM_OVERWORLD) {
            return own == null ? null : this.ownSpans(own);
        }

        var derivation = this.offer.derivation();
        return derivation == null ? null : derivation.apply(this.overworld.get().spans(), this.netherScale.get());
    }

    private @Nullable FlatShape declaredSpans(DimensionShape shape) {
        ShapeOptions chosenOptions = this.declared.commit(this.options);
        if (chosenOptions == null) {
            return null;
        }

        boolean isOverworldItself = this.offer.dimension() == LevelStem.OVERWORLD;
        return DeclaredShapes.resolve(shape, new DimensionContext(this.offer.dimension(), chosenOptions,
                isOverworldItself ? null : this.overworld.get().spans(), this.scales.of(LevelStem.OVERWORLD),
                this.scales.of(this.offer.dimension())));
    }

    private @Nullable FlatShape ownSpans(ShapeKind kind) {
        DimensionShape shape = kind.getBuiltIn();
        if (shape == null) {
            return null;
        }

        ShapeOptions size = this.parameters.commit(ShapeOptions.DEFAULT);
        return size == null ? null : shape.spans(new DimensionContext(this.offer.dimension(), size, null,
                this.scales.of(LevelStem.OVERWORLD), this.scales.of(this.offer.dimension())));
    }

    private @Nullable ShapeKind getOwn() {
        return this.chosen.get().own();
    }

    private ShapeKind getResolvedKind() {
        ShapeKind own = this.getOwn();
        return own == null || own == ShapeKind.FROM_OVERWORLD ? ShapeKind.of(this.getResolvedSpans()) : own;
    }

    private @Nullable Identifier getResolvedShape() {
        DimensionShape shape = this.chosen.get().declared();
        if (shape != null) {
            return shape.getId();
        }

        ShapeKind resolved = this.getResolvedKind();
        Identifier fromOverworld = this.getOwn() == ShapeKind.FROM_OVERWORLD ? this.overworld.get().shape() : null;
        if (resolved == ShapeKind.NORMAL || fromOverworld != null) {
            return resolved == ShapeKind.NORMAL ? null : fromOverworld;
        }

        return resolved == ShapeKind.TORUS ? DeclaredShapes.TORUS : DeclaredShapes.CYLINDER;
    }

    private @Nullable Identifier getChosenShape() {
        ShapeChoice choice = this.chosen.get();
        DimensionShape declared = choice.declared();
        if (declared != null) {
            return declared.getId();
        }

        DimensionShape own = choice.own() == null ? null : choice.own().getBuiltIn();
        return own == null ? null : own.getId();
    }

    private ShapeOptions getShownParameters(DimensionLayout layout) {
        FlatShape spans = layout.shape() == null ? layout.spans() : null;
        ShapeOptions shown = ShapeOptions.DEFAULT;
        for (DimensionShape shape : getParameterShapes()) {
            ShapeOptions source = (spans == null ? Optional.<ShapeOptions>empty() : shape.optionsFor(spans))
                    .orElseGet(() -> shape.defaultOptions(this.offer.dimension()));
            for (DimensionOption<?> parameter : shape.getParameters()) {
                shown = copied(parameter, source, shown);
            }
        }

        return shown;
    }

    private static List<DimensionOption<?>> getParameters() {
        List<DimensionOption<?>> parameters = new ArrayList<>();
        for (DimensionShape shape : getParameterShapes()) {
            shape.getParameters().forEach(parameter -> parameters.add(parameter.forShapes(List.of(shape.getId()))));
        }

        return parameters;
    }

    private static List<DimensionShape> getParameterShapes() {
        List<DimensionShape> shapes = new ArrayList<>(ShapeKind.getBuiltIns());
        shapes.addAll(DeclaredShapes.getAll());
        return shapes;
    }

    private static <T> ShapeOptions copied(DimensionOption<T> parameter, ShapeOptions from, ShapeOptions into) {
        return into.with(parameter.getOption(), from.get(parameter.getOption()));
    }

    private @Nullable Integer spawnWidthChunks(Direction.Axis axis) {
        ShapeChoice choice = this.chosen.get();
        DimensionShape shape = choice.declared();
        ShapeKind own = choice.own();
        FlatShape spans = shape != null ? this.declaredSpans(shape) : own == null ? null : this.ownSpans(own);
        if (spans == null) {
            return null;
        }

        return spans.bounds().isLooped(axis) ? spans.bounds().getWidthChunks(axis) : WorldLoopSizes.ENDLESS_AXIS_WIDTH;
    }
}
