package com.exoticworlds.bridge.v2.feature.client;

import com.exoticworlds.api.v2.feature.OptionFeatureV2;
import com.exoticworlds.api.v2.feature.client.FeatureViewV2;
import com.exoticworlds.api.v2.feature.client.LineViewV2;
import com.exoticworlds.bridge.v2.feature.FeatureBridge;
import com.exoticworlds.bridge.v2.geometry.ShapeSpansAdapter;
import com.exoticworlds.shape.feature.client.FeatureView;
import com.exoticworlds.shape.feature.client.FeatureViews;
import com.exoticworlds.shape.feature.client.LineView;

public final class FeatureViewBridge {
    public static <T> void registerLine(OptionFeatureV2<T> feature, LineViewV2<T> line) {
        FeatureViews.register(FeatureBridge.optionOf(feature), internal(line));
    }

    public static <T> void register(OptionFeatureV2<T> feature, FeatureViewV2<T> view) {
        FeatureViews.register(FeatureBridge.optionOf(feature), internal(view));
    }

    public static <T> LineView<T> internal(LineViewV2<T> line) {
        return new LineView<>((value, spans) -> line.line(value, ShapeSpansAdapter.loopSpans(spans)));
    }

    public static <T> FeatureView<T> internal(FeatureViewV2<T> view) {
        return panel -> view.extend(new FeaturePanelView<>(panel));
    }

    private FeatureViewBridge() {
    }
}
