package com.exoticworlds.engine.client;

import java.util.function.Consumer;

import com.exoticworlds.accessors.ClientBoundsHolder;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.engine.level.PublishedShapes;
import com.exoticworlds.engine.net.ComponentPositions;
import com.exoticworlds.engine.net.DimensionChoicesPayload;
import com.exoticworlds.engine.net.PositionRowsPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class WorldLoopClientNetwork {
    private static volatile Consumer<DimensionChoicesPayload> dimensionChoicesOpener = choices -> {
    };

    public static void setDimensionShape(ResourceKey<Level> dimension, FlatShape shape) {
        WorldFold fold = WorldFolds.of(shape);
        PublishedShapes.publish(dimension, fold);
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && level.dimension() == dimension) {
            ((ClientBoundsHolder) level).exotic_worlds$setClientBounds(fold);
        }
    }

    public static void declarePositionRows(PositionRowsPayload rows) {
        SyncedTagFold.declare(rows.blockEntities());
        ComponentPositions.declare(rows.components());
    }

    public static void setDimensionChoicesOpener(Consumer<DimensionChoicesPayload> opener) {
        dimensionChoicesOpener = opener;
    }

    public static void openDimensionChoices(DimensionChoicesPayload choices) {
        dimensionChoicesOpener.accept(choices);
    }

    private WorldLoopClientNetwork() {
    }
}
