package com.exoticworlds.client.engine;

import java.util.List;

import com.exoticworlds.accessors.ClientBoundsHolder;
import com.exoticworlds.client.shape.custom.WorldDimensionsScreen;
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

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

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
        List<WorldDimensionsScreen.Row> rows = choices.rows().stream()
                .map(row -> new WorldDimensionsScreen.Row(row.stem(), row.lock().map(WorldLoopClientNetwork::lockOf)
                        .orElse(null), row.entry()))
                .toList();
        Minecraft.getInstance().gui.setScreen(new WorldDimensionsScreen(rows,
                chosen -> Platforms.get().sendDimensionChoicesUpdate(new DimensionChoicesUpdatePayload(chosen))));
    }

    private static WorldDimensionsScreen.Lock lockOf(DimensionChoices.Refusal refusal) {
        return switch (refusal) {
            case VANILLA, UNKNOWN -> WorldDimensionsScreen.Lock.VANILLA;
            case DECIDED -> WorldDimensionsScreen.Lock.CHOSEN;
            case GENERATED -> WorldDimensionsScreen.Lock.GENERATED;
        };
    }

    private WorldLoopClientNetwork() {
    }
}
