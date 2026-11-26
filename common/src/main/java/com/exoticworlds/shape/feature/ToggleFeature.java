package com.exoticworlds.shape.feature;

import java.util.List;

import com.exoticworlds.core.option.ShapeOption;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.shape.option.OptionPart;
import com.mojang.serialization.Codec;

public abstract class ToggleFeature extends OptionFeature<Boolean> {
    private static final boolean OFF = false;

    protected ToggleFeature(String key, int position, String captionKey) {
        super(new ShapeOption<>(key, position, Codec.BOOL, OFF), captionKey, List.of(OptionPart.toggle()));
    }

    @Override
    public final boolean isOn(ShapeOptions options) {
        return options.get(getOption());
    }
}
