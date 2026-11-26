package com.exoticworlds.bridge.v2.feature;

import com.exoticworlds.api.v2.feature.SpawnV2;
import com.exoticworlds.core.feature.SpawnMoment;

import net.minecraft.world.level.dimension.LevelStem;

final class SpawnView extends MomentView implements SpawnV2 {
    SpawnView(SpawnMoment moment) {
        super(moment.fold(), LevelStem.OVERWORLD, moment.options());
    }
}
