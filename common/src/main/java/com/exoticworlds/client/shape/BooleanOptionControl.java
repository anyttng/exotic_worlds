package com.exoticworlds.client.shape;

import com.exoticworlds.api.v1.client.WorldOptionContext;
import com.exoticworlds.api.v1.client.WorldOptionControl;
import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.option.WorldOption;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;

public final class BooleanOptionControl implements WorldOptionControl {
    private static final String HINT_SUFFIX = "_hint";

    private final WorldOption<Boolean> option;
    private final Component label;
    private final Component hint;

    private boolean chosen;

    public BooleanOptionControl(WorldOptionContext context, WorldOption<Boolean> option, String labelKey) {
        this.option = option;
        this.label = Component.translatable(labelKey);
        this.hint = Component.translatable(labelKey + HINT_SUFFIX);
        this.chosen = context.options().get(option);
    }

    @Override
    public void addWidgets(Font font, LinearLayout contents) {
        contents.addChild(CycleButton.onOffBuilder(this.chosen)
                .withTooltip(value -> Tooltip.create(this.hint))
                .create(0, 0, LoopSizeControls.FIELD_WIDTH, LoopSizeControls.FIELD_HEIGHT, this.label,
                        (button, value) -> this.chosen = value));
    }

    @Override
    public GenerationOptions commit(GenerationOptions options) {
        return options.with(this.option, this.chosen);
    }
}
