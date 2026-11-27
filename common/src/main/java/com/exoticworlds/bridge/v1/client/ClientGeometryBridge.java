package com.exoticworlds.bridge.v1.client;

import java.util.Optional;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.bridge.v1.geometry.GeometryBridge;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.engine.level.PublishedShapes;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class ClientGeometryBridge {

    public static Optional<ToroidalShape> of(ClientLevel level) {
        WorldFold fold = WorldLoopAttachments.getWrappedClientBoundsFold(level);
        return fold == null ? Optional.empty() : Optional.of(GeometryBridge.of(fold));
    }

    public static Optional<ToroidalShape> of(ResourceKey<Level> dimension) {
        WorldFold fold = PublishedShapes.getWrappedFold(dimension);
        return fold == null ? Optional.empty() : Optional.of(GeometryBridge.of(fold));
    }

    private ClientGeometryBridge() {
    }
}
