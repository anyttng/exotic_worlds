package com.exoticworlds.api.v2.feature.client;

import com.exoticworlds.api.v2.geometry.LoopSpansV2;

import net.minecraft.network.chat.Component;

/**
 * One line of text under a feature's controls on the World Shape Settings screen, worked out from the value the
 * player has chosen and the spans the dimension comes to: a preview of what the value will do. Register it with
 * {@link FeatureViewsV2#registerLine}.
 *
 * @param <T> the type of the feature's value
 */
@FunctionalInterface
public interface LineViewV2<T> {
    /**
     * The line for {@code value} on a dimension of {@code spans}. An empty component hides the line. It is asked
     * every frame the page is drawn, so keep it cheap or remember the last answer.
     */
    Component line(T value, LoopSpansV2 spans);
}
