package com.exoticworlds.bridge.v2.client;


import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v2.client.DimensionOptionLinesV2;
import com.exoticworlds.api.v2.option.DimensionOptionV2;
import com.exoticworlds.bridge.v2.option.ShapeOptionsAdapter;
import com.exoticworlds.bridge.v2.shape.ShapeSpansAdapter;
import com.exoticworlds.shape.feature.client.FeatureViews;
import com.exoticworlds.shape.feature.client.LineView;


public final class OptionLinesBridge {

    public static <T> void register(DimensionOptionV2<T> option, DimensionOptionLinesV2.Line<T> line) {
        FeatureViews.register(ShapeOptionsAdapter.fromV2(option.getOption()),
                new LineView<T>((value, shape) -> line.line(value, ShapeSpansAdapter.loopSpans(shape))));
    }

    public static <T> DimensionOptionLinesV2.@Nullable Line<T> of(DimensionOptionV2<T> option) {
        return FeatureViews.of(ShapeOptionsAdapter.fromV2(option.getOption())) instanceof LineView<T> view
                ? (value, spans) -> view.line().apply(value, ShapeSpansAdapter.flatShape(spans))
                : null;
    }

    private OptionLinesBridge() {
    }
}
