package com.exoticworlds.client.shape.layout;

import java.util.List;

import com.ascendiacore.gui.base.property.Value;
import com.ascendiacore.gui.theme.Theme;
import com.ascendiacore.gui.widget.cyclebutton.CycleButton;

import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

final class OptionFields {
    private static final String HINT_ON = "on";
    private static final String HINT_OFF = "off";

    static CycleButton<Boolean> toggle(Theme theme, String captionKey, Value<Boolean> chosen) {
        CycleButton<Boolean> button = new CycleButton<>(theme.button(), Component.translatable(captionKey),
                () -> List.of(false, true), chosen,
                on -> on ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
        button.setHint(theme.tooltip(), captionKey, on -> on ? HINT_ON : HINT_OFF);
        return button;
    }

    private OptionFields() {
    }
}
