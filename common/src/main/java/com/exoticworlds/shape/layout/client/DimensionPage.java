package com.exoticworlds.shape.layout.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
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
import com.exoticworlds.core.world.WorldLoopSizes;
import com.exoticworlds.shape.dimension.DeclaredShapes;
import com.exoticworlds.shape.dimension.DimensionContext;
import com.exoticworlds.shape.dimension.DimensionShape;
import com.exoticworlds.shape.layout.DimensionLayout;
import com.exoticworlds.shape.layout.DimensionMode;
import com.exoticworlds.shape.option.DeclaredOptions;
import com.exoticworlds.shape.option.DimensionOption;

import net.minecraft.core.Direction;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

final class DimensionPage {
    static final int GAP_PIXELS = 4;

    private final DimensionOffer offer;
    private final CoordinateScales scales;
    private final Supplier<DimensionLayout> overworld;
    private final Value<ShapeChoice> chosen = Value.of(ShapeChoice.of(DimensionMode.NORMAL));
    private final Value<Integer> netherScale = Value.of(NetherScales.DEFAULT);
    private final OptionControls parameters;
    private final OptionControls declared;
    private final Column page = new Column();
    private ShapeOptions options;

    DimensionPage(DimensionOffer offer, Theme theme, FillStyle fill, CoordinateScales scales,
            Supplier<DimensionLayout> overworld, DimensionLayout shown, @Nullable Component lockReason,
            @Nullable Screen creation) {
        this.offer = offer;
        this.scales = scales;
        this.overworld = overworld;
        this.options = shown.options();
        this.parameters = new OptionControls(theme, fill, offer.dimension(), getParameters(), this::getChosenShape,
                this::getResolvedSpans, creation);
        this.declared = new OptionControls(theme, fill, offer.dimension(), getAdditionalOptions(offer.dimension()),
                this::getResolvedShape, this::getResolvedSpans, creation);
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
        this.chosen.set(shape != null
                ? ShapeChoice.of(shape)
                : ShapeChoice.of(layout.mode() == DimensionMode.FROM_OVERWORLD ? DimensionMode.FROM_OVERWORLD : DimensionMode.NORMAL));
        this.parameters.show(this.getShownParameters(layout));
        this.declared.show(this.options);
    }

    void showNetherScale(int scale) {
        this.netherScale.set(scale);
    }

    int getNetherScale() {
        return NetherScales.normalize(this.netherScale.get(), this.getNetherScales());
    }

    @Nullable DimensionLayout layout() {
        ShapeChoice choice = this.chosen.get();
        if (choice.mode() == DimensionMode.NORMAL) {
            return DimensionLayout.normal(this.options);
        }

        ShapeOptions chosenOptions = this.getChosenOptions();
        if (!this.page.areFieldsValid() || chosenOptions == null) {
            return null;
        }

        DimensionShape shape = choice.shape();
        if (shape == null) {
            return DimensionLayout.fromOverworld(chosenOptions);
        }

        return this.getSizeError().getString().isEmpty()
                ? DimensionLayout.shaped(shape.getId(), this.getShapeSpans(shape), chosenOptions)
                : null;
    }

    private @Nullable ShapeOptions getChosenOptions() {
        if (this.getResolvedShape() == null) {
            return this.options;
        }

        return this.declared.commit(this.options);
    }

    private void build(Theme theme, FillStyle fill, @Nullable Component lockReason) {
        CycleButton<ShapeChoice> shape = new CycleButton<>(theme.button(),
                Component.translatable(SettingsLines.SHAPE_KEY), this::getChoices, this.chosen, ShapeChoice::getName);
        shape.setValueHints(theme.tooltip(), SettingsLines.SHAPE_KEY, ShapeChoice::getHint);

        Column additional = new Column();
        additional.setGapPixels(GAP_PIXELS);
        additional.add(Heading.of(fill, SettingsLines.ADDITIONAL));
        additional.add(this.declared.getSection());

        additional.setVisible(this.declared::isAnyOffered);

        this.page.setGapPixels(GAP_PIXELS);
        this.page.add(Heading.of(fill, this.offer.title()));
        if (lockReason != null) {
            this.page.add(new Label(fill, LabelRole.MUTED, lockReason));
            this.page.setEnabled(false);
        }

        this.page.add(shape);
        this.page.add(this.netherScaleField(theme, fill));
        this.page.add(this.derivedLine(fill));
        this.page.add(this.sizeErrorLine(fill));
        this.page.add(this.parameters.getSection());
        this.page.add(additional);
    }

    private List<ShapeChoice> getChoices() {
        List<ShapeChoice> choices = new ArrayList<>();
        this.offer.modes().forEach(mode -> choices.add(ShapeChoice.of(mode)));
        DeclaredShapes.getAll().forEach(shape -> choices.add(ShapeChoice.of(shape)));
        return choices;
    }

    private Field netherScaleField(Theme theme, FillStyle fill) {
        CycleButton<Integer> scale = new CycleButton<>(theme.button(), SettingsLines.NETHER_SCALE, this::getNetherScales,
                this.netherScale, picked -> SettingsLines.netherScaleName(this.getNetherScale()));
        scale.showValueOnly();
        Field field = new Field(fill, SettingsLines.NETHER_SCALE, scale);
        field.setValueLine(() -> SettingsLines.shapeSummary(this.getResolvedSpans()));
        field.setVisible(() -> this.offer.hasNetherScale() && this.getMode() == DimensionMode.FROM_OVERWORLD);
        return field;
    }

    private Label derivedLine(FillStyle fill) {
        Label line = new Label(fill, LabelRole.MUTED, Component.empty());
        line.setText(() -> SettingsLines.shapeSummary(this.getResolvedSpans()));
        line.setVisible(() -> this.isShowingOwnSummary()
                || (this.isDerivationKnown() && !this.offer.hasNetherScale() && this.getMode() == DimensionMode.FROM_OVERWORLD));
        return line;
    }

    private Label sizeErrorLine(FillStyle fill) {
        Label line = new Label(fill, LabelRole.ERROR, Component.empty());
        line.setText(this::getSizeError);
        line.setVisible(() -> !this.getSizeError().getString().isEmpty());
        return line;
    }

    private Component getSizeError() {
        DimensionShape shape = this.chosen.get().shape();
        FlatShape spans = shape == null ? null : this.getShapeSpans(shape);
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
        return spans == null ? List.of(this.netherScale.get()) : NetherScales.allowedFor(spans);
    }

    private @Nullable FlatShape getResolvedSpans() {
        ShapeChoice choice = this.chosen.get();
        DimensionShape shape = choice.shape();
        if (shape != null) {
            return this.getShapeSpans(shape);
        }

        if (choice.mode() != DimensionMode.FROM_OVERWORLD) {
            return null;
        }

        var derivation = this.offer.derivation();
        return derivation == null ? null : derivation.apply(this.overworld.get(), this.netherScale.get());
    }

    private @Nullable FlatShape getShapeSpans(DimensionShape shape) {
        ShapeOptions additional = this.declared.commit(this.options);
        ShapeOptions chosenOptions = additional == null ? null : this.parameters.commit(additional);
        if (chosenOptions == null) {
            return null;
        }

        boolean isOverworldItself = this.offer.dimension() == LevelStem.OVERWORLD;
        return DeclaredShapes.resolve(shape, new DimensionContext(this.offer.dimension(), chosenOptions,
                isOverworldItself ? null : this.overworld.get().spans(), this.scales.of(LevelStem.OVERWORLD),
                this.scales.of(this.offer.dimension())));
    }

    private boolean isShowingOwnSummary() {
        DimensionShape shape = this.chosen.get().shape();
        return shape != null && shape.getParameters().isEmpty();
    }

    private @Nullable DimensionMode getMode() {
        return this.chosen.get().mode();
    }

    private @Nullable Identifier getResolvedShape() {
        return this.getMode() == DimensionMode.FROM_OVERWORLD ? this.overworld.get().shape() : this.getChosenShape();
    }

    private @Nullable Identifier getChosenShape() {
        DimensionShape shape = this.chosen.get().shape();
        return shape == null ? null : shape.getId();
    }

    private ShapeOptions getShownParameters(DimensionLayout layout) {
        FlatShape spans = layout.spans();
        ShapeOptions shown = ShapeOptions.DEFAULT;
        for (DimensionShape shape : DeclaredShapes.getAll()) {
            ShapeOptions source = (spans == null ? Optional.<ShapeOptions>empty() : shape.optionsFor(spans))
                    .orElseGet(() -> shape.defaultOptions(this.offer.dimension()));
            for (DimensionOption<?> parameter : shape.getParameters()) {
                shown = copied(parameter, source, shown);
            }
        }

        return shown;
    }

    private static List<OptionControls.Offer> getParameters() {
        List<OptionControls.Offer> parameters = new ArrayList<>();
        for (DimensionShape shape : DeclaredShapes.getAll()) {
            Identifier owner = shape.getId();
            shape.getParameters().forEach(parameter -> parameters.add(new OptionControls.Offer(parameter, owner::equals)));
        }

        return parameters;
    }

    private static List<OptionControls.Offer> getAdditionalOptions(ResourceKey<LevelStem> dimension) {
        Map<DimensionOption<?>, Predicate<Identifier>> offered = new LinkedHashMap<>();
        for (DimensionShape shape : DeclaredShapes.getAll()) {
            Identifier owner = shape.getId();
            shape.getOptions().stream()
                    .filter(scoped -> scoped.scope().contains(dimension))
                    .forEach(scoped -> offered.merge(scoped.option(), owner::equals, Predicate::or));
        }

        DeclaredOptions.getAll().stream()
                .filter(declared -> declared.scope().contains(dimension))
                .forEach(declared -> offered.merge(declared.option(), declared::isForShape, Predicate::or));
        return offered.entrySet().stream()
                .map(entry -> new OptionControls.Offer(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static <T> ShapeOptions copied(DimensionOption<T> parameter, ShapeOptions from, ShapeOptions into) {
        return into.with(parameter.getOption(), from.get(parameter.getOption()));
    }
}
