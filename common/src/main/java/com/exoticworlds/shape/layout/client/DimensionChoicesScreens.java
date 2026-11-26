package com.exoticworlds.shape.layout.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exoticworlds.core.world.DimensionTable;
import com.exoticworlds.engine.gen.DimensionChoices;
import com.exoticworlds.engine.net.DimensionChoicesPayload;
import com.exoticworlds.engine.net.DimensionChoicesUpdatePayload;
import com.exoticworlds.platform.Platforms;
import com.exoticworlds.shape.layout.LayoutDimensions;
import com.exoticworlds.shape.layout.WorldLayout;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public final class DimensionChoicesScreens {
    public static void open(DimensionChoicesPayload choices) {
        Map<ResourceKey<LevelStem>, DimensionTable.Entry> entries = new HashMap<>();
        Map<ResourceKey<LevelStem>, DimensionLock> locks = new HashMap<>();
        List<ResourceKey<LevelStem>> modStems = new ArrayList<>();
        Map<ResourceKey<LevelStem>, Double> scales = new HashMap<>();
        for (DimensionChoicesPayload.Row row : choices.rows()) {
            entries.put(row.stem(), row.entry());
            scales.put(row.stem(), row.coordinateScale());
            row.lock().ifPresent(refusal -> locks.put(row.stem(), lockOf(refusal)));
            if (!DimensionTable.isVanillaStem(row.stem())) {
                modStems.add(row.stem());
            }
        }

        CoordinateScales coordinateScales = new CoordinateScales(scales);
        Minecraft.getInstance().setScreen(WorldShapeSettingsScreen.forExistingWorld(
                LayoutDimensions.ofRows(entries), modStems,
                coordinateScales, locks,
                chosen -> send(chosen, modStems, locks)));
    }

    private static void send(WorldLayout chosen, List<ResourceKey<LevelStem>> modStems,
            Map<ResourceKey<LevelStem>, DimensionLock> locks) {
        Map<ResourceKey<LevelStem>, DimensionTable.Entry> choices = new HashMap<>();
        for (ResourceKey<LevelStem> stem : modStems) {
            if (!locks.containsKey(stem)) {
                choices.put(stem, LayoutDimensions.entryOf(chosen.getModDimension(stem)));
            }
        }

        if (!choices.isEmpty()) {
            Platforms.get().sendDimensionChoicesUpdate(new DimensionChoicesUpdatePayload(choices));
        }
    }

    private static DimensionLock lockOf(DimensionChoices.Refusal refusal) {
        return switch (refusal) {
            case VANILLA, UNKNOWN -> DimensionLock.VANILLA;
            case DECIDED -> DimensionLock.CHOSEN;
            case GENERATED -> DimensionLock.GENERATED;
        };
    }

    private DimensionChoicesScreens() {
    }
}
