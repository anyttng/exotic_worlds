package com.exoticworlds.bridge.v2.feature;

import com.exoticworlds.api.v2.feature.RandomStateV2;
import com.exoticworlds.core.feature.GenerationFeature;
import com.exoticworlds.core.feature.RandomStateMoment;

import net.minecraft.world.level.levelgen.RandomState;

final class RandomStateView extends MomentView implements RandomStateV2 {
    private final RandomStateMoment moment;
    private final GenerationFeature feature;

    RandomStateView(RandomStateMoment moment, GenerationFeature feature) {
        super(moment.fold(), moment.dimension(), moment.fold().getOptions());
        this.moment = moment;
        this.feature = feature;
    }

    @Override
    public RandomState getRandomState() {
        return this.moment.randomState();
    }

    @Override
    public int getSeaLevel() {
        return this.moment.seaLevel();
    }

    @Override
    public void setNoiseOffset(double offset) {
        this.moment.setNoiseOffset(this.feature, offset);
    }
}
