package com.exoticworlds.engine.net;

import com.exoticworlds.core.fold.DeckTransformation;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
import com.exoticworlds.core.fold.cuboid.CuboidEdge;
import com.exoticworlds.core.fold.cuboid.CuboidFace;
import com.exoticworlds.core.fold.cuboid.CuboidFixtures;
import com.exoticworlds.core.fold.deck.DeckGroupFold;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.engine.seam.ClientPosition;
import com.exoticworlds.engine.seam.MirrorWriter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

final class WalkedMirrors {
    private static final CuboidAtlas BOX =
            CuboidAtlas.of(24, 16, 20, CuboidFixtures.MIN_CHUNK_X, CuboidFixtures.MIN_CHUNK_Z);
    private static final double INSIDE_THE_EDGE_BLOCKS = 8.0;
    private static final double STEP_ACROSS_BLOCKS = 16.0;
    private static final int STRIP_MIN_CHUNK = -16;
    private static final int STRIP_MAX_CHUNK = 16;
    private static final double STRIP_X_BLOCKS = 10.5;
    private static final double SEAM_Z_BLOCKS = STRIP_MAX_CHUNK * 16.0;

    record Walked(WorldFold fold, ClientPosition mirror, Vec3 serverPoint, DeckTransformation toTheClient) {
        Vec3 seatOf(Vec3 server) {
            return this.toTheClient.apply(server);
        }

        BlockPos seatOf(BlockPos server) {
            return this.toTheClient.apply(server);
        }

        Vec3 clientPoint() {
            return new Vec3(this.mirror.getX(), 0.0, this.mirror.getZ());
        }
    }

    static Walked acrossAQuarterTurn() {
        CuboidFace top = CuboidFixtures.face(BOX, CuboidFace.Side.TOP);
        CuboidEdge topToWest = BOX.edges().stream()
                .filter(edge -> edge.first() == CuboidFace.Side.TOP && edge.firstSide() == Direction.WEST)
                .findFirst()
                .orElseThrow();
        Vec3 start = new Vec3(top.minBlockX() + INSIDE_THE_EDGE_BLOCKS, 0.0, top.minBlockZ() + top.depthBlocks() / 2.0);
        Vec3 across = start.add(-STEP_ACROSS_BLOCKS, 0.0, 0.0);
        DeckTransformation toTheServer = new DeckTransformation(topToWest.transform());
        return walked(CuboidFixtures.cuboidFold(BOX), start, across, toTheServer);
    }

    static Walked acrossAMirror() {
        WorldLoopBounds zOnly = WorldLoopBounds.UNBOUNDED.with(Direction.Axis.Z,
                new WorldLoopBounds.AxisBounds.Looped(STRIP_MIN_CHUNK, STRIP_MAX_CHUNK));
        WorldFold fold = new DeckGroupFold(ShapeFixtures.mirrored(zOnly, Direction.Axis.X, 0), ShapeOptions.DEFAULT);
        Vec3 start = new Vec3(STRIP_X_BLOCKS, 0.0, SEAM_Z_BLOCKS - INSIDE_THE_EDGE_BLOCKS);
        Vec3 across = start.add(0.0, 0.0, STEP_ACROSS_BLOCKS);
        return walked(fold, start, across, fold.foldTransformation(across));
    }

    private static Walked walked(WorldFold fold, Vec3 start, Vec3 across, DeckTransformation toTheServer) {
        ClientPosition mirror = new ClientPosition();
        mirror.rebase(start.x, start.z, Level.OVERWORLD, fold);
        mirror.set(across.x, across.z, MirrorWriter.PLAYER_MOVE);
        return new Walked(fold, mirror, toTheServer.apply(across), toTheServer.inverse());
    }

    private WalkedMirrors() {
    }
}
