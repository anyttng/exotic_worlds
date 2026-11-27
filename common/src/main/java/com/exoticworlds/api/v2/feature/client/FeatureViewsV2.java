package com.exoticworlds.api.v2.feature.client;

import com.exoticworlds.api.v2.feature.OptionFeatureV2;
import com.exoticworlds.bridge.v2.feature.client.FeatureViewBridge;

/**
 * What a feature shows on the World Shape Settings screen beyond its controls. A feature has at most one view. Call
 * these from your mod's client entry point, after {@code FeaturesV2.register}. A dedicated server never loads this
 * package.
 */
public final class FeatureViewsV2 {
    /**
     * Shows one line of text under {@code feature}'s controls.
     *
     * @throws IllegalStateException if the feature was never registered, if it already has a view, or if a world has
     *        already started
     */
    public static <T> void registerLine(OptionFeatureV2<T> feature, LineViewV2<T> line) {
        FeatureViewBridge.registerLine(feature, line);
    }

    private FeatureViewsV2() {
    }
}
