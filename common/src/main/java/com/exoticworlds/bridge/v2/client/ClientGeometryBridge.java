package com.exoticworlds.bridge.v2.client;

import java.util.Optional;

import com.exoticworlds.api.v2.geometry.WorldGeometry;
import com.exoticworlds.bridge.v2.geometry.GeometryBridge;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.level.PublishedShapes;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class ClientGeometryBridge {

    public static Optional<WorldGeometry> of(ClientLevel level) {
        return GeometryBridge.ofNullable(WorldLoopAttachments.wrappedClientBoundsFoldOf(level));
    }

    public static Optional<WorldGeometry> of(ResourceKey<Level> dimension) {
        return GeometryBridge.ofNullable(PublishedShapes.wrappedFoldOf(dimension));
    }

    private ClientGeometryBridge() {
    }
}
