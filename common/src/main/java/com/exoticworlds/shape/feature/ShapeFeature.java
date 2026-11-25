package com.exoticworlds.shape.feature;

import java.util.List;

import com.exoticworlds.core.option.ShapeOption;
import com.exoticworlds.shape.option.DimensionOption;
import com.exoticworlds.shape.option.OptionPart;

public abstract class ShapeFeature<T> {
    private final ShapeOption<T> option;
    private final DimensionOption<T> shown;

    protected ShapeFeature(ShapeOption<T> option, String captionKey, List<OptionPart<T>> parts) {
        this.option = option;
        this.shown = DimensionOption.of(option, captionKey, parts);
    }

    public final ShapeOption<T> getOption() {
        return this.option;
    }

    public final DimensionOption<T> getShown() {
        return this.shown;
    }
}
