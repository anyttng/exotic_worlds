package com.exoticworlds.api.v2.geometry.client;

import java.util.Optional;

import com.exoticworlds.api.v2.geometry.WorldGeometryV2;
import com.exoticworlds.bridge.v2.geometry.client.ClientGeometryBridge;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Entry point for reading a level's geometry on the client. A client level's own engine believes the world is
 * infinite, which is what keeps rendering and chunk loading vanilla across the seam, so
 * {@link com.exoticworlds.api.v2.geometry.WorldGeometriesV2} answers empty there. The bounds the server synced ride
 * apart on the client level, and this reads them.
 *
 * <p>Client coordinates near the seam run whole world widths from the server's: fold a coordinate before keying
 * storage by it, and take {@code nearestCopy} or {@code shortestDelta} against the player before measuring direction
 * or distance.</p>
 */
public final class ClientGeometriesV2 {

    /**
     * The geometry the server declared for {@code level}, or empty when the level does not wrap or the bounds have
     * not arrived yet: they are synced on login and dimension change, before the first chunk.
     */
    public static Optional<WorldGeometryV2> of(ClientLevel level) {
        return ClientGeometryBridge.of(level);
    }

    /**
     * The geometry the server declared for {@code dimension}, whether or not the player is standing in it: what a
     * fullscreen map browsing another dimension folds by. Empty when that dimension does not wrap, or when the server
     * never published it: every wrapped level arrives on login, before the first chunk.
     */
    public static Optional<WorldGeometryV2> of(ResourceKey<Level> dimension) {
        return ClientGeometryBridge.of(dimension);
    }

    private ClientGeometriesV2() {
    }
}
