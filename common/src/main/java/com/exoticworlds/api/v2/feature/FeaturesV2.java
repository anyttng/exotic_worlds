package com.exoticworlds.api.v2.feature;

import com.exoticworlds.bridge.v2.feature.FeatureBridge;

/**
 * Where a mod hands its features to Exotic Worlds. Register every feature once, while the mod is constructed. The
 * order of registration is the order features run in within one moment and the order their options are listed on the
 * World Shape Settings screen.
 */
public final class FeaturesV2 {

    /**
     * Registers {@code feature}. Until it is added to a shape it acts nowhere.
     *
     * @throws IllegalStateException if the server has already started, or a feature with this id is registered
     */
    public static void register(FeatureV2 feature) {
        FeatureBridge.register(feature);
    }

    private FeaturesV2() {
    }
}
