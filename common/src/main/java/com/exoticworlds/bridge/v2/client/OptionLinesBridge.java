package com.exoticworlds.bridge.v2.client;

import java.util.function.BiFunction;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v2.client.DimensionOptionLinesV2;
import com.exoticworlds.api.v2.option.DimensionOptionV2;
import com.exoticworlds.bridge.v2.option.ShapeOptionsAdapter;
import com.exoticworlds.bridge.v2.shape.ShapeSpansAdapter;
import com.exoticworlds.client.shape.OptionLines;
import com.exoticworlds.core.FlatShape;

import net.minecraft.network.chat.Component;

public final class OptionLinesBridge {

    public static <T> void register(DimensionOptionV2<T> option, DimensionOptionLinesV2.Line<T> line) {
        OptionLines.register(ShapeOptionsAdapter.fromV2(option.option()),
                (value, shape) -> line.line(value, ShapeSpansAdapter.loopSpans(shape)));
    }

    public static <T> DimensionOptionLinesV2.@Nullable Line<T> of(DimensionOptionV2<T> option) {
        BiFunction<T, FlatShape, Component> line = OptionLines.of(ShapeOptionsAdapter.fromV2(option.option()));
        return line == null ? null : (value, spans) -> line.apply(value, ShapeSpansAdapter.flatShape(spans));
    }

    private OptionLinesBridge() {
    }
}
