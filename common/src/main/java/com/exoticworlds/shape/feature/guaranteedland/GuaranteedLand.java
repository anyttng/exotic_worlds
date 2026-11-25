package com.exoticworlds.shape.feature.guaranteedland;

import com.exoticworlds.shape.feature.ToggleFeature;

public final class GuaranteedLand extends ToggleFeature {
    public static final String KEY = "guaranteed_land";

    private static final String CAPTION_KEY = "gui.exotic_worlds.world_shape_settings.guaranteed_land";

    private static final int POSITION = 1;

    public static final GuaranteedLand FEATURE = new GuaranteedLand();

    private GuaranteedLand() {
        super(KEY, POSITION, CAPTION_KEY);
    }
}
