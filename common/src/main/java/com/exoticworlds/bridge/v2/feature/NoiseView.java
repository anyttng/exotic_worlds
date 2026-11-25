package com.exoticworlds.bridge.v2.feature;

import com.exoticworlds.api.v2.feature.NoiseV2;
import com.exoticworlds.core.feature.NoiseMoment;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

final class NoiseView extends MomentView implements NoiseV2 {
    private final NoiseMoment moment;

    NoiseView(NoiseMoment moment) {
        super(moment.fold(), moment.dimension(), moment.fold().getOptions());
        this.moment = moment;
    }

    @Override
    public Holder<NormalNoise> getNoise() {
        return this.moment.noise();
    }

    @Override
    public int getLayerCount() {
        return this.moment.stack().layers.length;
    }

    @Override
    public double getLayerFrequency(int layer) {
        return this.moment.stack().layers[layer].frequency();
    }

    @Override
    public double getScale() {
        return this.moment.xzScale();
    }

    @Override
    public double getVerticalShare() {
        return this.moment.verticalShare();
    }
}
