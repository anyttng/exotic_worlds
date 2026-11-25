package com.exoticworlds.shape.feature.guaranteedland;

import com.exoticworlds.core.feature.NoiseFeature;
import com.exoticworlds.core.feature.NoiseMoment;
import com.exoticworlds.core.feature.RandomStateFeature;
import com.exoticworlds.core.feature.RandomStateMoment;
import com.exoticworlds.shape.feature.ToggleFeature;


public final class GuaranteedLand extends ToggleFeature implements RandomStateFeature, NoiseFeature {
    public static final String KEY = "guaranteed_land";

    private static final String CAPTION_KEY = "gui.exotic_worlds.world_shape_settings.guaranteed_land";

    private static final int POSITION = 1;

    public static final GuaranteedLand FEATURE = new GuaranteedLand();

    private GuaranteedLand() {
        super(KEY, POSITION, CAPTION_KEY);
    }

    @Override
    public void atRandomState(RandomStateMoment moment) {
        CoastFieldLift.lift(this, moment);
    }

    @Override
    public boolean isOffsetting(NoiseMoment moment) {
        return moment.noise().unwrapKey().filter(CoastFields::isCoast).isPresent();
    }
}
