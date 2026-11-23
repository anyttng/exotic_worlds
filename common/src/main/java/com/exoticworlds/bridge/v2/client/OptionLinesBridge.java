package com.exoticworlds.bridge.v2.client;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v2.client.DimensionOptionLines;
import com.exoticworlds.api.v2.option.DimensionOption;
import com.exoticworlds.client.shape.OptionLines;
import com.exoticworlds.shape.ShapeSpansAdapter;

public final class OptionLinesBridge {

    public static <T> void register(DimensionOption<T> option, DimensionOptionLines.Line<T> line) {
        OptionLines.register(option, (value, shape) -> line.line(value, ShapeSpansAdapter.loopSpans(shape)));
    }

    public static <T> DimensionOptionLines.@Nullable Line<T> of(DimensionOption<T> option) {
        var line = OptionLines.of(option);
        return line == null ? null : (value, spans) -> line.apply(value, ShapeSpansAdapter.flatShape(spans));
    }

    private OptionLinesBridge() {
    }
}
