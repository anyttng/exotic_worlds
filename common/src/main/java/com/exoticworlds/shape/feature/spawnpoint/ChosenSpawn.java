package com.exoticworlds.shape.feature.spawnpoint;

import java.util.List;

import com.exoticworlds.core.feature.SpawnFeature;
import com.exoticworlds.core.feature.SpawnMoment;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.SpawnPoint;
import com.exoticworlds.shape.feature.OptionFeature;

public final class ChosenSpawn extends OptionFeature<SpawnPoint> implements SpawnFeature {
    public static final String CAPTION_KEY = "gui.exotic_worlds.world_shape_settings.spawn";

    public static final ChosenSpawn FEATURE = new ChosenSpawn();

    private ChosenSpawn() {
        super(SpawnPoint.OPTION, CAPTION_KEY, List.of());
    }

    @Override
    public boolean isOn(ShapeOptions options) {
        return options.get(SpawnPoint.OPTION).isCustom();
    }

    @Override
    public SpawnPoint spawnPointIn(SpawnMoment moment) {
        return moment.options().get(SpawnPoint.OPTION);
    }
}
