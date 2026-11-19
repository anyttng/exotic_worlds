package com.exoticworlds.client.shape.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.ascendiacore.gui.base.component.GuiComponent;
import com.ascendiacore.gui.base.property.Value;
import com.ascendiacore.gui.layout.Column;
import com.ascendiacore.gui.theme.Theme;
import com.ascendiacore.gui.widget.cyclebutton.CycleButton;
import com.ascendiacore.gui.widget.label.Label;
import com.ascendiacore.gui.widget.label.model.LabelRole;
import com.ascendiacore.gui.widget.numberfield.NumberField;
import com.ascendiacore.gui.widget.numberfield.model.Bounds;
import com.ascendiacore.gui.widget.panel.model.FillStyle;
import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.api.v2.client.DimensionOptionLines;
import com.exoticworlds.api.v2.option.DimensionOption;
import com.exoticworlds.api.v2.option.OptionPart;
import com.exoticworlds.shape.DeclaredOptions;

import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

final class OptionControls {
    private static final String HINT_ON = "on";
    private static final String HINT_OFF = "off";
    private static final String KEY_SEPARATOR = ".";
    private static final int OVERFLOW_ROOM = 1;

    private final Theme theme;
    private final FillStyle fill;
    private final ResourceKey<LevelStem> dimension;
    private final Supplier<@Nullable ResourceLocation> shape;
    private final Supplier<@Nullable LoopSpans> spans;
    private final List<Bound<?>> bound = new ArrayList<>();
    private final Column section = new Column();

    OptionControls(Theme theme, FillStyle fill, ResourceKey<LevelStem> dimension, Supplier<@Nullable ResourceLocation> shape,
            Supplier<@Nullable LoopSpans> spans) {
        this.theme = theme;
        this.fill = fill;
        this.dimension = dimension;
        this.shape = shape;
        this.spans = spans;
        this.section.setGap(LoopFields.GAP);
        for (DimensionOption<?> declared : DeclaredOptions.all()) {
            this.bound.add(new Bound<>(declared));
        }
    }

    Column section() {
        return this.section;
    }

    boolean anyOffered() {
        return this.bound.stream().anyMatch(Bound::offered);
    }

    void show(GenerationOptions options) {
        this.bound.forEach(option -> option.show(options));
    }

    @Nullable GenerationOptions commit(GenerationOptions into) {
        GenerationOptions chosen = into;
        for (Bound<?> option : this.bound) {
            chosen = option.commit(chosen);
            if (chosen == null) {
                return null;
            }
        }

        return chosen;
    }

    private record NumberPart<T>(OptionPart.WholeNumber<T> part, NumberField field) {
    }

    private final class Bound<T> {
        private final DimensionOption<T> declared;
        private final List<Runnable> shownValues = new ArrayList<>();
        private final List<NumberPart<T>> numbers = new ArrayList<>();
        private T value;

        Bound(DimensionOption<T> declared) {
            this.declared = declared;
            this.value = declared.option().defaultValue();
            declared.parts().forEach(this::add);
            DimensionOptionLines.Line<T> line = DimensionOptionLines.of(declared);
            if (line != null) {
                this.addLine(line);
            }
        }

        boolean offered() {
            ResourceLocation chosen = OptionControls.this.shape.get();
            return chosen != null && this.declared.offeredFor(chosen, OptionControls.this.dimension);
        }

        void show(GenerationOptions options) {
            this.value = options.get(this.declared.option());
            this.shownValues.forEach(Runnable::run);
        }

        @Nullable GenerationOptions commit(GenerationOptions into) {
            if (!this.offered()) {
                return into;
            }

            T chosen = this.chosen();
            return chosen == null ? null : into.with(this.declared.option(), chosen);
        }

        private @Nullable T chosen() {
            T chosen = this.value;
            for (NumberPart<T> number : this.numbers) {
                if (!number.part().shownWhen().test(this.value)) {
                    continue;
                }

                Integer typed = number.field().value();
                if (typed == null) {
                    return null;
                }

                chosen = number.part().write().apply(chosen, typed);
            }

            return chosen;
        }

        private void add(OptionPart<T> part) {
            String captionKey = part.key().isEmpty()
                    ? this.declared.captionKey()
                    : this.declared.captionKey() + KEY_SEPARATOR + part.key();
            GuiComponent control = switch (part) {
                case OptionPart.Toggle<T> toggle -> this.cycle(captionKey, List.of(false, true),
                        on -> on ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF,
                        on -> on ? HINT_ON : HINT_OFF, toggle.read(), toggle.write());
                case OptionPart.Choice<T, ?> choice -> this.choice(captionKey, choice);
                case OptionPart.WholeNumber<T> number -> this.number(captionKey, number);
            };
            control.setVisible(() -> this.offered() && part.shownWhen().test(this.value));
            OptionControls.this.section.add(control);
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
            Optional<Bounds> bounds = Optional.of(new Bounds(part.min(), part.max()));
            NumberField field = new NumberField(OptionControls.this.fill, OptionControls.this.theme.textField(),
                    Component.translatable(captionKey), () -> bounds, () -> {
                    });
            field.setSigned(part.min() < 0);
            field.setMaxLength(Math.max(String.valueOf(part.min()).length(), String.valueOf(part.max()).length())
                    + OVERFLOW_ROOM);
            field.setValue(part.read().apply(this.value));
            this.numbers.add(new NumberPart<>(part, field));
            this.shownValues.add(() -> field.setValue(part.read().apply(this.value)));
            return field;
        }

        private void addLine(DimensionOptionLines.Line<T> line) {
            Label label = new Label(OptionControls.this.fill, LabelRole.MUTED, Component.empty());
            label.setText(() -> this.lineText(line));
            label.setVisible(() -> this.offered() && !this.lineText(line).getString().isEmpty());
            OptionControls.this.section.add(label);
        }

        private Component lineText(DimensionOptionLines.Line<T> line) {
            T chosen = this.chosen();
            LoopSpans dimensionSpans = OptionControls.this.spans.get();
            return chosen == null || dimensionSpans == null ? Component.empty() : line.line(chosen, dimensionSpans);
        }
    }
}
