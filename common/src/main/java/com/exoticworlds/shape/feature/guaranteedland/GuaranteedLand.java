package com.exoticworlds.shape.feature.guaranteedland;

import com.exoticworlds.core.feature.RandomStateFeature;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.shape.feature.ToggleFeature;

import net.minecraft.world.level.levelgen.RandomState;

public final class GuaranteedLand extends ToggleFeature implements RandomStateFeature {
    public static final String KEY = "guaranteed_land";

    private static final String CAPTION_KEY = "gui.exotic_worlds.world_shape_settings.guaranteed_land";

    private static final int POSITION = 1;

    public static final GuaranteedLand FEATURE = new GuaranteedLand();

    private GuaranteedLand() {
        super(KEY, POSITION, CAPTION_KEY);
    }

    @Override
    public void atRandomState(RandomState randomState, WorldFold fold, int seaLevel) {
        CoastFieldLift.lift(randomState, fold, seaLevel);
    }
}
