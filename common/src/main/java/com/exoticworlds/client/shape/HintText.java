package com.exoticworlds.client.shape;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class HintText {
    private static final String PARAGRAPH = "\n\n";

    private final MutableComponent text = Component.empty();
    private boolean started;

    public HintText paragraph(Component paragraph) {
        return this.append(paragraph);
    }

    public Component build() {
        return this.text;
    }

    private HintText append(Component paragraph) {
        if (this.started) {
            this.text.append(PARAGRAPH);
        }

        this.text.append(paragraph);
        this.started = true;
        return this;
    }
}
