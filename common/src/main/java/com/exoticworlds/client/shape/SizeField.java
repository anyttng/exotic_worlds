package com.exoticworlds.client.shape;

import java.util.function.IntPredicate;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.CoordinateConstants;
import com.exoticworlds.core.WorldLoopSizes;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class SizeField {
    static final String TOO_LARGE_KEY = "gui.exotic_worlds.toroidal_settings.too_large";
    static final int FIELD_MAX_LENGTH = 7;

    private static final String SIZE_LABEL_KEY = "gui.exotic_worlds.toroidal_settings.size";
    private static final Component HINT = Component.translatable("gui.exotic_worlds.toroidal_settings.hint");
    private static final String EFFECTIVE_KEY = "gui.exotic_worlds.toroidal_settings.effective";
    private static final String END_SIZE_LABEL_KEY = "gui.exotic_worlds.toroidal_settings.end_size";
    private static final String END_EFFECTIVE_KEY = "gui.exotic_worlds.toroidal_settings.end_effective";
    private static final Component END_HINT = Component.translatable("gui.exotic_worlds.toroidal_settings.end_hint");
    static final String TOO_SMALL_KEY = "gui.exotic_worlds.toroidal_settings.too_small";

    private final String labelKey;
    private final String effectiveKey;
    private final Component hint;
    private final int minChunks;
    private final IntPredicate inRange;

    private String text;
    private @Nullable Integer effective;
    private @Nullable DigitsEditBox edit;

    private SizeField(String labelKey, String effectiveKey, Component hint, int minChunks, IntPredicate inRange,
            int chunkWidth) {
        this.labelKey = labelKey;
        this.effectiveKey = effectiveKey;
        this.hint = hint;
        this.minChunks = minChunks;
        this.inRange = inRange;
        this.text = String.valueOf(chunkWidth);
    }

    public static SizeField world(int chunkWidth) {
        return world(SIZE_LABEL_KEY, chunkWidth);
    }

    public static SizeField world(String labelKey, int chunkWidth) {
        return new SizeField(labelKey, EFFECTIVE_KEY, HINT, WorldLoopSizes.MIN_CHUNK_WIDTH, WorldLoopSizes::isInRange,
                chunkWidth);
    }

    public static SizeField end(int chunkWidth) {
        return end(END_SIZE_LABEL_KEY, chunkWidth);
    }

    public static SizeField end(String labelKey, int chunkWidth) {
        return new SizeField(labelKey, END_EFFECTIVE_KEY, END_HINT, WorldLoopSizes.END_MIN_CHUNK_WIDTH,
                WorldLoopSizes::isEndInRange, chunkWidth);
    }

    public void add(Font font, LinearLayout contents, Runnable onEdited) {
        DigitsEditBox box = new DigitsEditBox(font, LoopSizeControls.FIELD_WIDTH, LoopSizeControls.FIELD_HEIGHT,
                this.label());
        box.setMaxLength(FIELD_MAX_LENGTH);
        box.setValue(this.text);
        box.setResponder(value -> {
            this.text = value;
            onEdited.run();
        });
        contents.addChild(CommonLayouts.labeledElement(font, box, this.label()));
        this.edit = box;
    }

    public void update() {
        if (this.edit == null) {
            return;
        }

        Integer sizeChunks = this.edit.number();
        this.effective = sizeChunks != null && this.inRange.test(sizeChunks) ? sizeChunks : null;
        this.edit.setTooltip(Tooltip.create(this.effective != null
                ? this.effectiveLine(this.effective).copy().append(CommonComponents.NEW_LINE).append(this.hint)
                : this.boundHint(sizeChunks)));
    }

    public void setValue(int chunkWidth) {
        this.text = String.valueOf(chunkWidth);
        if (this.edit != null) {
            this.edit.setValue(this.text);
        }
    }

    public @Nullable Integer effective() {
        return this.effective;
    }

    public Component effectiveLine(int chunkWidth) {
        return Component.translatable(this.effectiveKey, chunkWidth, chunkWidth * CoordinateConstants.CHUNK_WIDTH);
    }

    private Component label() {
        return Component.translatable(this.labelKey, this.minChunks, WorldLoopSizes.MAX_CHUNK_WIDTH);
    }

    private Component boundHint(@Nullable Integer sizeChunks) {
        if (sizeChunks == null) {
            return this.hint;
        }

        Component bound = sizeChunks < this.minChunks
                ? Component.translatable(TOO_SMALL_KEY, this.minChunks)
                : Component.translatable(TOO_LARGE_KEY, WorldLoopSizes.MAX_CHUNK_WIDTH);
        return bound.copy().append(CommonComponents.NEW_LINE).append(this.hint);
    }
}
