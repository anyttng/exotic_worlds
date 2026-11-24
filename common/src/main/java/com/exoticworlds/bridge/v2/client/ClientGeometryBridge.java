package com.exoticworlds.bridge.v2.client;

import java.util.Optional;

import com.exoticworlds.api.v2.geometry.WorldGeometryV2;
import com.exoticworlds.bridge.v2.geometry.GeometryBridge;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.engine.level.PublishedShapes;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class ClientGeometryBridge {

    public static Optional<WorldGeometryV2> of(ClientLevel level) {
        return GeometryBridge.ofNullable(WorldLoopAttachments.getWrappedClientBoundsFold(level));
    }

    public static Optional<WorldGeometryV2> of(ResourceKey<Level> dimension) {
        return GeometryBridge.ofNullable(PublishedShapes.getWrappedFold(dimension));
    }

    private ClientGeometryBridge() {
    }
}
