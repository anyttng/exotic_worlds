package com.exoticworlds.shape.feature.guaranteednethercomplexes;

import java.util.List;

import com.exoticworlds.core.feature.StructureStartsFeature;
import com.exoticworlds.core.gen.AddedStart;
import com.exoticworlds.core.gen.StructureStartSet;
import com.exoticworlds.shape.feature.ToggleFeature;

public final class GuaranteedNetherComplexes extends ToggleFeature implements StructureStartsFeature {
    public static final String KEY = "guaranteed_nether_complexes";

    private static final String CAPTION_KEY = "gui.exotic_worlds.world_shape_settings.guaranteed_nether_complexes";

    private static final int POSITION = 2;

    public static final GuaranteedNetherComplexes FEATURE = new GuaranteedNetherComplexes();

    private GuaranteedNetherComplexes() {
        super(KEY, POSITION, CAPTION_KEY);
    }

    @Override
    public List<AddedStart> startsIn(StructureStartSet set) {
        return NetherComplexStarts.starts(set);
    }
}
