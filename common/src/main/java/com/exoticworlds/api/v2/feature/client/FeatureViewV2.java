package com.exoticworlds.api.v2.feature.client;

import com.exoticworlds.api.v2.ExperimentalV2;

/**
 * Widgets of your own under a feature's controls on the World Shape Settings screen. Register it with {@link
 * FeatureViewsV2#register}. For one line of text {@link LineViewV2} is enough and takes no widget.
 *
 * <p>The screen is built from Ascendia Core widgets, so the panel takes and gives Ascendia Core types and your mod
 * compiles against Ascendia Core. That toolkit may change between releases, so this type is experimental.</p>
 *
 * @param <T> the type of the feature's value
 */
@ExperimentalV2
@FunctionalInterface
public interface FeatureViewV2<T> {
    /** Called once per dimension page as the screen is built. Create the widgets and add them to {@code panel}. */
    void extend(FeaturePanelV2<T> panel);
}
