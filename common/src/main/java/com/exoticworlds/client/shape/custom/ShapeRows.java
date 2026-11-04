package com.exoticworlds.client.shape.custom;

import java.util.function.Consumer;

import com.exoticworlds.client.shape.LoopSizeControls;
import com.exoticworlds.shape.custom.ModDimension;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;

final class ShapeRows {
    static final Component CUSTOMIZE = Component.translatable("selectWorld.customizeType");
    static final int CUSTOMIZE_WIDTH = 98;
    static final int ROW_SPACING = 4;
    static final int CHOICE_WIDTH = LoopSizeControls.FIELD_WIDTH - CUSTOMIZE_WIDTH - ROW_SPACING;

    static LinearLayout modRow(ModDimension dimension, ModStemChoice choice, Consumer<ModStemChoice> onPick,
            Runnable onCustomize) {
        LinearLayout row = LinearLayout.horizontal().spacing(ROW_SPACING);
        Button customize = Button.builder(CUSTOMIZE, button -> onCustomize.run())
                .width(CUSTOMIZE_WIDTH)
                .build();
        row.addChild(CycleButton.builder(ShapeLabels::choice, choice)
                .withValues(ModStemChoice.values())
                .displayOnlyValue()
                .withTooltip(picked -> Tooltip.create(ShapeLabels.choiceHint(picked)))
                .create(0, 0, CHOICE_WIDTH, LoopSizeControls.FIELD_HEIGHT, ShapeLabels.dimension(dimension),
                        (button, picked) -> {
                            onPick.accept(picked);
                            customize.active = picked.isCustomizable();
                        }));
        customize.active = choice.isCustomizable();
        row.addChild(customize);
        return row;
    }

    static Button lockedRow(Component value, Component reason) {
        Button locked = Button.builder(value, button -> {
                })
                .width(LoopSizeControls.FIELD_WIDTH)
                .tooltip(Tooltip.create(reason))
                .build();
        locked.active = false;
        return locked;
    }

    private ShapeRows() {
    }
}
