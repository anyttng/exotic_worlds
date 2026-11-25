package com.exoticworlds.shape.feature.guaranteednethercomplexes;

import com.exoticworlds.shape.feature.ToggleFeature;

public final class GuaranteedNetherComplexes extends ToggleFeature {
    public static final String KEY = "guaranteed_nether_complexes";

    private static final String CAPTION_KEY = "gui.exotic_worlds.world_shape_settings.guaranteed_nether_complexes";

    private static final int POSITION = 2;

    public static final GuaranteedNetherComplexes FEATURE = new GuaranteedNetherComplexes();

    private GuaranteedNetherComplexes() {
        super(KEY, POSITION, CAPTION_KEY);
    }
}
