package com.exoticworlds.shape.feature;

import java.util.List;

import com.exoticworlds.core.option.ShapeOption;
import com.exoticworlds.shape.option.DimensionOption;
import com.exoticworlds.shape.option.OptionPart;

public abstract class OptionFeature<T> extends ShapeFeature {
    private final ShapeOption<T> option;
    private final T newWorldValue;
    private final DimensionOption<T> shown;

    protected OptionFeature(ShapeOption<T> option, String captionKey, List<OptionPart<T>> parts) {
        this(option, option.defaultValue(), captionKey, parts);
    }

    protected OptionFeature(ShapeOption<T> option, T newWorldValue, String captionKey, List<OptionPart<T>> parts) {
        super(option.key());
        this.option = option;
        this.newWorldValue = newWorldValue;
        this.shown = DimensionOption.of(option, captionKey, parts);
    }

    public final ShapeOption<T> getOption() {
        return this.option;
    }

    public final T getNewWorldValue() {
        return this.newWorldValue;
    }

    public final DimensionOption<T> getShown() {
        return this.shown;
    }
}
