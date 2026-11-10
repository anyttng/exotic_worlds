package com.exoticworlds.client.shape;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class DigitsEditBox extends EditBox {
    private static final char MINUS = '-';

    private final boolean signed;

    public DigitsEditBox(Font font, int width, int height, Component message) {
        this(font, width, height, message, false);
    }

    private DigitsEditBox(Font font, int width, int height, Component message, boolean signed) {
        super(font, width, height, message);
        this.signed = signed;
    }

    public static DigitsEditBox signed(Font font, int width, int height, Component message) {
        return new DigitsEditBox(font, width, height, message, true);
    }

    @Override
    public void insertText(String text) {
        String digits = text.chars().filter(this::accepts)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        if (!digits.isEmpty() || text.isEmpty()) {
            super.insertText(digits);
        }
    }

    private boolean accepts(int character) {
        return Character.isDigit(character) || (this.signed && character == MINUS);
    }

    public @Nullable Integer number() {
        try {
            return Integer.parseInt(this.getValue());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
