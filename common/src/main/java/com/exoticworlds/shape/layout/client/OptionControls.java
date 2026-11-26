package com.exoticworlds.shape.layout.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.ascendiacore.gui.base.component.GuiComponent;
import com.ascendiacore.gui.base.property.Value;
import com.ascendiacore.gui.layout.Column;
import com.ascendiacore.gui.layout.Row;
import com.ascendiacore.gui.layout.model.Length;
import com.ascendiacore.gui.theme.Theme;
import com.ascendiacore.gui.widget.cyclebutton.CycleButton;
import com.ascendiacore.gui.widget.numberfield.NumberField;
import com.ascendiacore.gui.widget.numberfield.model.Bounds;
import com.ascendiacore.gui.widget.panel.model.FillStyle;
import com.ascendiacore.gui.widget.toggleswitch.ToggleSwitch;
import com.exoticworlds.core.world.StemTerrain;
import com.exoticworlds.shape.feature.client.FeaturePanel;
import com.exoticworlds.shape.feature.client.FeatureView;
import com.exoticworlds.shape.feature.client.FeatureViews;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.shape.option.DimensionOption;
import com.exoticworlds.shape.option.NumberRange;
import com.exoticworlds.shape.option.OptionPart;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

final class OptionControls {
    private static final String KEY_SEPARATOR = ".";
    private static final int NUMBER_MAX_LENGTH = 7;
    private static final int ROW_GAP_PIXELS = 6;

    private final Theme theme;
    private final FillStyle fill;
    private final StemTerrain stem;
    private final Supplier<@Nullable Identifier> shape;
    private final Supplier<@Nullable FlatShape> spans;
    private final @Nullable Screen creation;
    private final List<Bound<?>> bound = new ArrayList<>();
    private final Column section = new Column();

    record Offer(DimensionOption<?> option, Predicate<Identifier> isForShape) {
    }

    OptionControls(Theme theme, FillStyle fill, StemTerrain stem, List<Offer> offered,
            Supplier<@Nullable Identifier> shape, Supplier<@Nullable FlatShape> spans, @Nullable Screen creation) {
        this.theme = theme;
        this.fill = fill;
        this.stem = stem;
        this.shape = shape;
        this.spans = spans;
        this.creation = creation;
        this.section.setGapPixels(DimensionPage.GAP_PIXELS);
        for (Offer offer : offered) {
            this.bound.add(this.bind(offer.option(), offer.isForShape()));
        }
    }

    Column getSection() {
        return this.section;
    }

    boolean isAnyOffered() {
        return this.bound.stream().anyMatch(Bound::isOffered);
    }

    void show(ShapeOptions options) {
        this.bound.forEach(option -> option.show(options));
    }

    @Nullable ShapeOptions commit(ShapeOptions into) {
        ShapeOptions chosen = into;
        for (Bound<?> option : this.bound) {
            chosen = option.commit(chosen);
            if (chosen == null) {
                return null;
            }
        }

        return chosen;
    }

    private <T> Bound<T> bind(DimensionOption<T> option, Predicate<Identifier> isForShape) {
        return new Bound<>(option, isForShape);
    }

    private static Bounds boundsOf(NumberRange range) {
        return new Bounds(range.min(), range.max());
    }

    private record NumberPart<T>(OptionPart.WholeNumber<T> part, NumberField field) {
    }

    private final class Bound<T> implements FeaturePanel<T> {
        private final DimensionOption<T> declared;
        private final Predicate<Identifier> isForShape;
        private final List<Runnable> shownValues = new ArrayList<>();
        private final List<NumberPart<T>> numbers = new ArrayList<>();
        private @Nullable Supplier<@Nullable T> ownValue;
        private T value;
        private boolean isShowing;
        private boolean isResolvingSpans;

        Bound(DimensionOption<T> declared, Predicate<Identifier> isForShape) {
            this.declared = declared;
            this.isForShape = isForShape;
            this.value = declared.getOption().defaultValue();
            declared.getParts().forEach(part -> OptionControls.this.section.add(this.control(part)));
            FeatureView<T> view = FeatureViews.of(declared.getOption());
            if (view != null) {
                view.extendPanel(this);
            }
        }

        @Override
        public @Nullable T getValue() {
            return this.getChosen();
        }

        @Override
        public @Nullable FlatShape getSpans() {
            if (this.isResolvingSpans) {
                return OptionControls.this.spans.get();
            }

            this.isResolvingSpans = true;
            try {
                return OptionControls.this.spans.get();
            } finally {
                this.isResolvingSpans = false;
            }
        }

        @Override
        public @Nullable Screen getCreationScreen() {
            return OptionControls.this.creation;
        }

        @Override
        public Theme getTheme() {
            return OptionControls.this.theme;
        }

        @Override
        public FillStyle getFill() {
            return OptionControls.this.fill;
        }

        @Override
        public void addComponent(GuiComponent component) {
            OptionControls.this.section.add(component);
        }

        @Override
        public void onShow(Consumer<T> shown) {
            this.shownValues.add(() -> shown.accept(this.value));
        }

        @Override
        public void takeValueFrom(Supplier<@Nullable T> chosen) {
            this.ownValue = chosen;
        }

        @Override
        public boolean isOffered() {
            Identifier chosen = OptionControls.this.shape.get();
            return chosen != null && this.isForShape.test(chosen);
        }

        void show(ShapeOptions options) {
            this.isShowing = true;
            this.value = options.get(this.declared.getOption());
            this.shownValues.forEach(Runnable::run);
            this.isShowing = false;
        }

        @Nullable ShapeOptions commit(ShapeOptions into) {
            if (!this.isOffered()) {
                return into;
            }

            T chosen = this.getChosen();
            return chosen == null ? null : into.with(this.declared.getOption(), chosen);
        }

        private @Nullable T getChosen() {
            if (this.ownValue != null) {
                return this.isResolvingSpans ? this.value : this.ownValue.get();
            }

            T chosen = this.value;
            for (NumberPart<T> number : this.numbers) {
                if (!number.part().shownWhen().test(this.value)) {
                    continue;
                }

                Integer typed = number.field().getValue();
                if (typed == null) {
                    return null;
                }

                chosen = number.part().write().apply(chosen, typed);
            }

            return chosen;
        }

        private void takeTypedNumbers() {
            if (this.isShowing) {
                return;
            }

            for (NumberPart<T> number : this.numbers) {
                Integer typed = number.field().getValue();
                if (typed != null && number.part().shownWhen().test(this.value)) {
                    this.value = number.part().write().apply(this.value, typed);
                }
            }
        }

        private GuiComponent control(OptionPart<T> part) {
            String captionKey = part.key().isEmpty()
                    ? this.declared.getCaptionKey()
                    : this.declared.getCaptionKey() + KEY_SEPARATOR + part.key();
            GuiComponent control = switch (part) {
                case OptionPart.Toggle<T> toggle -> this.toggleSwitch(captionKey, toggle);
                case OptionPart.Choice<T, ?> choice -> this.choice(captionKey, choice);
                case OptionPart.WholeNumber<T> number -> this.number(captionKey, number);
                case OptionPart.Row<T> row -> this.row(row);
            };
            control.setVisible(() -> this.isOffered() && part.shownWhen().test(this.value));
            return control;
        }

        private GuiComponent row(OptionPart.Row<T> part) {
            Row row = new Row();
            row.setGapPixels(ROW_GAP_PIXELS);
            part.parts().forEach(child -> row.add(this.control(child), Length.fill()));
            return row;
        }

        private GuiComponent toggleSwitch(String captionKey, OptionPart.Toggle<T> part) {
            ToggleSwitch control = ToggleSwitch.following(OptionControls.this.theme.toggleSwitch(),
                    OptionControls.this.fill, Component.translatable(captionKey), () -> part.read().apply(this.value),
                    on -> this.value = part.write().apply(this.value, on));
            control.setHint(OptionControls.this.theme.tooltip(), captionKey);
            return control;
        }

        private <V> GuiComponent choice(String captionKey, OptionPart.Choice<T, V> part) {
            return this.cycle(captionKey, part.values(), picked -> {
                Component own = part.ownName().apply(picked);
                return own != null
                        ? own
                        : Component.translatable(captionKey + KEY_SEPARATOR + part.valueKey().apply(picked));
            }, part.valueKey(), part.read(), part.write());
        }

        private <V> GuiComponent cycle(String captionKey, List<V> values, Function<V, Component> valueName,
                Function<V, String> valueKey, Function<T, V> read, BiFunction<T, V, T> write) {
            Value<V> picked = Value.of(read.apply(this.value));
            CycleButton<V> button = new CycleButton<>(OptionControls.this.theme.button(),
                    Component.translatable(captionKey), () -> values, picked, valueName);
            button.setHint(OptionControls.this.theme.tooltip(), captionKey, valueKey);
            button.setOnChange(changed -> this.value = write.apply(this.value, changed));
            this.shownValues.add(() -> picked.set(read.apply(this.value)));
            return button;
        }

        private GuiComponent number(String captionKey, OptionPart.WholeNumber<T> part) {
            NumberRange shown = part.range().apply(OptionControls.this.stem, this.value);
            NumberField field = new NumberField(OptionControls.this.fill, OptionControls.this.theme.textField(),
                    Component.translatable(captionKey, shown.min(), shown.max()),
                    () -> Optional.of(boundsOf(part.range().apply(OptionControls.this.stem, this.value))),
                    this::takeTypedNumbers);
            field.setSigned(shown.min() < 0);
            field.setMaxCharacterCount(NUMBER_MAX_LENGTH);
            field.setValueLine(number -> part.valueLine().apply(this.value, number));
            field.setValue(part.read().apply(this.value));
            this.numbers.add(new NumberPart<>(part, field));
            this.shownValues.add(() -> field.setValue(part.read().apply(this.value)));
            return field;
        }
    }
}
