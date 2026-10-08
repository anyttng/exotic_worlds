package com.exoticworlds.client.engine;

import com.exoticworlds.accessors.ClientBoundsHolder;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.engine.net.ComponentPositions;
import com.exoticworlds.engine.net.PositionRowsPayload;

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
            ((ClientBoundsHolder) level).toroidal$setClientBounds(fold);
        }
    }

    public static void declare(PositionRowsPayload rows) {
        SyncedTagFold.declare(rows.blockEntities());
        ComponentPositions.declare(rows.components());
    }

    private WorldLoopClientNetwork() {
    }
}
