package com.exoticworlds.api.v1;

import java.util.Optional;
import java.util.OptionalDouble;

import com.exoticworlds.bridge.v1.geometry.GeometryBridge;
import com.exoticworlds.bridge.v1.seam.SeamBridge;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Entry point for reading a level's toroidal geometry on the logical server (or any {@link Level} whose own
 * engine knows its bounds). On the client the level is deliberately told the world is infinite, so its geometry
 * is read through {@link ExoticWorldsClientApi} instead.
 */
public final class ExoticWorldsApi {

    /**
     * The toroidal shape of {@code level}, or empty when no axis of that level loops. The view is immutable and
     * cheap; callers may hold it for as long as the level lives.
     */
    public static Optional<ToroidalShape> shapeOf(Level level) {
        return GeometryBridge.of(level);
    }

    /**
     * How far {@code player} has traveled along {@code axis} toward the next lap of the dimension they are in,
     * in blocks, signed with the direction of travel. Pacing back and forth cancels out, crossing the seam does
     * not count as a world width, and one whole width is taken off each time a lap closes. Where crossing the Z
     * seam also shifts X, the X total leaves out the shift a Z lap carries, so walking one Z lap moves it by
     * nothing. Empty when that
     * dimension does not wrap on that axis; {@link Direction.Axis#Y} is always empty.
     */
    public static OptionalDouble travelOf(ServerPlayer player, Direction.Axis axis) {
        return SeamBridge.travelOf(player, axis);
    }

    private ExoticWorldsApi() {
    }
}
