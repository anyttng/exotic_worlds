package com.exoticworlds.shape.option;

import java.util.List;

import com.exoticworlds.core.option.ShapeOption;

public final class DimensionOption<T> {
    private final ShapeOption<T> option;
    private final String captionKey;
    private final List<OptionPart<T>> parts;

    private DimensionOption(ShapeOption<T> option, String captionKey, List<OptionPart<T>> parts) {
        this.option = option;
        this.captionKey = captionKey;
        this.parts = List.copyOf(parts);
    }

    public static <T> DimensionOption<T> of(ShapeOption<T> option, String captionKey, List<OptionPart<T>> parts) {
        return new DimensionOption<>(option, captionKey, parts);
    }

    public ShapeOption<T> getOption() {
        return this.option;
    }

    public String getCaptionKey() {
        return this.captionKey;
    }

    public List<OptionPart<T>> getParts() {
        return this.parts;
    }
}
