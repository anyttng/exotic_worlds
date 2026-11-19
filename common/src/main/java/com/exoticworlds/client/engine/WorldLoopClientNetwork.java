package com.exoticworlds.client.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exoticworlds.accessors.ClientBoundsHolder;
import com.exoticworlds.client.shape.layout.DimensionLock;
import com.exoticworlds.client.shape.layout.ModStem;
import com.exoticworlds.client.shape.layout.WorldShapeSettingsScreen;
import com.exoticworlds.core.DimensionTable;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.engine.gen.DimensionChoices;
import com.exoticworlds.engine.level.PublishedShapes;
import com.exoticworlds.engine.net.ComponentPositions;
import com.exoticworlds.engine.net.DimensionChoicesPayload;
import com.exoticworlds.engine.net.DimensionChoicesUpdatePayload;
import com.exoticworlds.engine.net.PositionRowsPayload;
import com.exoticworlds.platform.Platforms;
import com.exoticworlds.shape.layout.LayoutDimensions;
import com.exoticworlds.shape.layout.WorldLayout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;

public final class WorldLoopClientNetwork {
    private static final double NO_OVERWORLD_SCALE = 0.0;

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
        List<ModStem> modStems = new ArrayList<>();
        double overworldScale = overworldScaleOf(choices);
        for (DimensionChoicesPayload.Row row : choices.rows()) {
            entries.put(row.stem(), row.entry());
            row.lock().ifPresent(refusal -> locks.put(row.stem(), lockOf(refusal)));
            if (!DimensionTable.isVanillaStem(row.stem())) {
                modStems.add(new ModStem(row.stem(), overworldScale, row.coordinateScale()));
            }
        }

        Minecraft.getInstance().gui.setScreen(WorldShapeSettingsScreen.forExistingWorld(
                LayoutDimensions.ofRows(entries), modStems, locks, chosen -> send(chosen, modStems, locks)));
    }

    private static void send(WorldLayout chosen, List<ModStem> modStems,
            Map<ResourceKey<LevelStem>, DimensionLock> locks) {
        Map<ResourceKey<LevelStem>, DimensionTable.Entry> choices = new HashMap<>();
        for (ModStem stem : modStems) {
            if (!locks.containsKey(stem.key())) {
                choices.put(stem.key(), LayoutDimensions.entryOf(chosen.modDimension(stem.key())));
            }
        }

        if (!choices.isEmpty()) {
            Platforms.get().sendDimensionChoicesUpdate(new DimensionChoicesUpdatePayload(choices));
        }
    }

    private static double overworldScaleOf(DimensionChoicesPayload choices) {
        return choices.rows().stream()
                .filter(row -> row.stem().equals(LevelStem.OVERWORLD))
                .mapToDouble(DimensionChoicesPayload.Row::coordinateScale)
                .findFirst()
                .orElse(NO_OVERWORLD_SCALE);
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
