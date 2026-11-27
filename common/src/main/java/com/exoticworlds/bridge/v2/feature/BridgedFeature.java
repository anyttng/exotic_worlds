package com.exoticworlds.bridge.v2.feature;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v2.feature.FeatureV2;
import com.exoticworlds.core.feature.GenerationFeature;
import com.exoticworlds.shape.feature.ShapeFeature;

final class BridgedFeature extends ShapeFeature {
    private final String namespace;
    private final FeatureMoments moments;

    BridgedFeature(FeatureV2 feature) {
        super(FeatureBridge.keyOf(feature));
        this.namespace = feature.getId().getNamespace();
        this.moments = new FeatureMoments(this, feature);
    }

    @Override
    public String getNamespace() {
        return this.namespace;
    }

    @Override
    public <F extends GenerationFeature> @Nullable F asMoment(Class<F> moment) {
        return this.moments.of(moment);
    }
}
