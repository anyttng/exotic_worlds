package com.exoticworlds.api.v2.geometry;

import java.util.Optional;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.world.level.Level;

/**
 * Entry point for reading a level's geometry on the logical server, or for any {@link Level} whose own engine knows
 * its bounds. On the client the level is deliberately told the world is infinite, so its geometry is read through
 * {@link com.exoticworlds.api.v2.client.ClientGeometries} instead.
 */
public final class WorldGeometries {

    /**
     * The geometry of {@code level}, or empty when that level does not wrap. The view is immutable and cheap; callers
     * may hold it for as long as the level lives.
     */
    public static Optional<WorldGeometry> of(Level level) {
        WorldFold fold = WorldLoopAttachments.wrappedFoldOf(level);
        return fold == null ? Optional.empty() : Optional.of(fold.geometryView());
    }

    private WorldGeometries() {
    }
}
