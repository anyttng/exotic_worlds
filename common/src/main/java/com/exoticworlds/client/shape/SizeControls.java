package com.exoticworlds.client.shape;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.layouts.LinearLayout;

public interface SizeControls {

    default void addPresets(LinearLayout contents) {
    }

    void addFields(Font font, LinearLayout contents);

    void refresh();

    boolean isComplete();
}
