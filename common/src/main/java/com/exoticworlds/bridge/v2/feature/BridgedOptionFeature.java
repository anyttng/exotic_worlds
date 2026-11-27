package com.exoticworlds.bridge.v2.feature;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v2.feature.OptionFeatureV2;
import com.exoticworlds.core.feature.GenerationFeature;
import com.exoticworlds.core.option.ShapeOption;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.shape.feature.OptionFeature;

final class BridgedOptionFeature<T> extends OptionFeature<T> {
    private static final int AFTER_THE_MODS_OWN_OPTIONS = Integer.MAX_VALUE;

    private final OptionFeatureV2<T> feature;
    private final FeatureMoments moments;

    BridgedOptionFeature(OptionFeatureV2<T> feature) {
        super(new ShapeOption<>(FeatureBridge.keyOf(feature), AFTER_THE_MODS_OWN_OPTIONS, feature.getCodec(),
                feature.getStoredDefault()), feature.getNewWorldValue(), feature.getCaptionKey(),
                FeatureBridge.toInternal(feature.getParts()));
        this.feature = feature;
        this.moments = new FeatureMoments(this, feature);
    }

    @Override
    public String getNamespace() {
        return this.feature.getId().getNamespace();
    }

    @Override
    public boolean isOn(ShapeOptions options) {
        return this.feature.isOn(options.get(getOption()));
    }

    @Override
    public <F extends GenerationFeature> @Nullable F asMoment(Class<F> moment) {
        return this.moments.of(moment);
    }
}
