package com.exoticworlds.shape.layout.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.ascendiacore.gui.base.component.GuiComponent;
import com.ascendiacore.gui.base.component.model.AlertColors;
import com.ascendiacore.gui.base.component.model.LevelColors;
import com.ascendiacore.gui.base.component.model.TooltipStyle;
import com.ascendiacore.gui.base.draw.TextMetrics;
import com.ascendiacore.gui.base.input.PointerInput;
import com.ascendiacore.gui.base.input.model.Modifiers;
import com.ascendiacore.gui.base.input.model.PointerButton;
import com.ascendiacore.gui.base.property.Value;
import com.ascendiacore.gui.widget.button.model.ButtonStyle;
import com.ascendiacore.gui.widget.segmented.SegmentedControl;
import com.exoticworlds.shape.option.OptionPart;

import net.minecraft.network.chat.Component;

class ChoiceControlTest {
    private static final ButtonStyle BUTTON = new ButtonStyle("test:normal", "test:hover", "test:disabled",
            "test:selected", "test:selected_hover", 0xFF112233, 0xFF445566, 0xFF778899, false);
    private static final TooltipStyle TOOLTIP = new TooltipStyle(null, 0xFFFFFFFF, 0xFFFFFF55, 0xFFAAAAAA,
            new AlertColors(0xFFFFAA00, 0xFFFF5555, 0xFFA0A0FF), new LevelColors(0xFF55FF55, 0xFFFFFF55, 0xFFFF5555),
            0xFF55FF55, 0xFF5A5A5A);
    private static final String CAPTION_KEY = "gui.example.axis";
    private static final List<String> VALUES = List.of("X", "Z");
    private static final TextMetrics METRICS = text -> text.getString().length();
    private static final int SIDE_PADDING_PIXELS = 8;
    private static final int NATURAL_WIDTH_PIXELS = VALUES.size() * (1 + 2 * SIDE_PADDING_PIXELS);
    private static final int HEIGHT_PIXELS = 20;
    private static final int WIDE_PIXELS = 100;
    private static final int SECOND_SEGMENT_X = 75;
    private static final int INSIDE_Y = 5;

    private final Value<String> picked = Value.of("X");
    private final List<String> changes = new ArrayList<>();

    @Test
    void segmentsShowWhereTheyFitTheWidth() {
        ChoiceControl<String> control = laidOut(NATURAL_WIDTH_PIXELS);
        assertTrue(segments(control).isVisible());
        assertFalse(cycle(control).isVisible());
    }

    @Test
    void cycleButtonShowsWhereTheSegmentsDoNotFit() {
        ChoiceControl<String> control = laidOut(NATURAL_WIDTH_PIXELS - 1);
        assertFalse(segments(control).isVisible());
        assertTrue(cycle(control).isVisible());
    }

    @Test
    void pickOnASegmentWritesTheValueOnce() {
        ChoiceControl<String> control = laidOut(WIDE_PIXELS);
        assertTrue(segments(control).tryPressPointer(new PointerInput(SECOND_SEGMENT_X, INSIDE_Y, PointerButton.LEFT,
                Modifiers.NONE)));
        assertEquals("Z", this.picked.get());
        assertEquals(List.of("Z"), this.changes);
    }

    private ChoiceControl<String> laidOut(int widthPixels) {
        OptionPart.Choice<String, String> part = new OptionPart.Choice<>(OptionPart.OWN_CAPTION, VALUES,
                Function.identity(), Component::literal, Function.identity(), (value, chosen) -> chosen,
                value -> true);
        ChoiceControl<String> control = new ChoiceControl<>(BUTTON, TOOLTIP, CAPTION_KEY, part, this.picked);
        control.setOnChange(this.changes::add);
        control.setPosition(0, 0);
        control.setSize(widthPixels, HEIGHT_PIXELS);
        control.layout(METRICS);
        return control;
    }

    private static GuiComponent segments(ChoiceControl<String> control) {
        return assertInstanceOf(SegmentedControl.class, control.getMountedComponents().getFirst());
    }

    private static GuiComponent cycle(ChoiceControl<String> control) {
        return control.getMountedComponents().getLast();
    }
}
