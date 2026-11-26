package com.exoticworlds.shape.layout.client;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import com.ascendiacore.gui.base.component.model.TooltipStyle;
import com.ascendiacore.gui.base.draw.TextMetrics;
import com.ascendiacore.gui.base.property.Value;
import com.ascendiacore.gui.layout.Container;
import com.ascendiacore.gui.layout.model.Axis;
import com.ascendiacore.gui.widget.button.model.ButtonStyle;
import com.ascendiacore.gui.widget.cyclebutton.CycleButton;
import com.ascendiacore.gui.widget.segmented.SegmentedControl;
import com.exoticworlds.shape.option.OptionPart;

import net.minecraft.network.chat.Component;

final class ChoiceControl<V> extends Container {
    private static final String KEY_SEPARATOR = ".";

    private final SegmentedControl<V> segments;
    private final CycleButton<V> cycle;
    private boolean isFitting;

    <T> ChoiceControl(ButtonStyle button, TooltipStyle tooltip, String captionKey, OptionPart.Choice<T, V> part,
            Value<V> picked) {
        super(Axis.VERTICAL);
        Component caption = Component.translatable(captionKey);
        List<V> values = part.values();
        Function<V, Component> valueName = value -> nameOf(captionKey, part, value);
        this.segments = new SegmentedControl<>(button, caption, () -> values, picked, valueName);
        this.segments.setHint(tooltip, captionKey, part.valueKey());
        this.segments.setVisible(() -> this.isFitting);
        this.cycle = new CycleButton<>(button, caption, () -> values, picked, valueName);
        this.cycle.showValueOnly();
        this.cycle.setHint(tooltip, captionKey, part.valueKey());
        this.cycle.setVisible(() -> !this.isFitting);
        add(this.segments);
        add(this.cycle);
    }

    void setOnChange(Consumer<V> onChange) {
        this.segments.setOnChange(onChange);
        this.cycle.setOnChange(onChange);
    }

    @Override
    public void layout(TextMetrics metrics) {
        this.isFitting = this.segments.naturalSize(metrics).widthPixels() <= getWidthPixels();
        super.layout(metrics);
    }

    private static <T, V> Component nameOf(String captionKey, OptionPart.Choice<T, V> part, V value) {
        Component own = part.ownName().apply(value);
        return own != null ? own : Component.translatable(captionKey + KEY_SEPARATOR + part.valueKey().apply(value));
    }
}
