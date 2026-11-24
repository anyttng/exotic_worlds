package com.exoticworlds.client.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exoticworlds.accessors.ClientBoundsHolder;
import com.exoticworlds.client.shape.layout.DimensionLock;
import com.exoticworlds.client.shape.layout.CoordinateScales;
import com.exoticworlds.client.shape.layout.WorldShapeSettingsScreen;
import com.exoticworlds.core.world.DimensionTable;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.engine.gen.DimensionChoices;
import com.exoticworlds.engine.level.PublishedShapes;
import com.exoticworlds.engine.net.ComponentPositions;
import com.exoticworlds.engine.net.DimensionChoicesPayload;
import com.exoticworlds.engine.net.DimensionChoicesUpdatePayload;
import com.exoticworlds.engine.net.PositionRowsPayload;
import com.exoticworlds.platform.Platforms;
import com.exoticworlds.shape.layout.LayoutDimensions;
import com.exoticworlds.shape.layout.ShapeRecognition;
import com.exoticworlds.shape.layout.WorldLayout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;

public final class WorldLoopClientNetwork {
    public static void apply(ResourceKey<Level> dimension, FlatShape shape) {
        WorldFold fold = WorldFolds.of(shape);
        PublishedShapes.publish(dimension, fold);
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && level.dimension() == dimension) {
            ((ClientBoundsHolder) level).exotic_worlds$setClientBounds(fold);
        }
    }

    public static void declare(PositionRowsPayload rows) {
        SyncedTagFold.declare(rows.blockEntities());
        ComponentPositions.declare(rows.components());
    }

    public static void openDimensionChoices(DimensionChoicesPayload choices) {
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
        Minecraft.getInstance().gui.setScreen(WorldShapeSettingsScreen.forExistingWorld(
                ShapeRecognition.recognize(LayoutDimensions.ofRows(entries), coordinateScales::of), modStems,
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

    private WorldLoopClientNetwork() {
    }
}
