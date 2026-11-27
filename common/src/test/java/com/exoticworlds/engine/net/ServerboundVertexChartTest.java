package com.exoticworlds.engine.net;

import static com.exoticworlds.engine.seam.VertexLoopWalk.EAST;
import static com.exoticworlds.engine.seam.VertexLoopWalk.HOP_ON_FRONT_AFTER_A_LOOP;
import static com.exoticworlds.engine.seam.VertexLoopWalk.STEP_ON_THE_FACE;
import static com.exoticworlds.engine.seam.VertexLoopWalk.assertNear;
import static com.exoticworlds.engine.seam.VertexLoopWalk.contextOf;
import static com.exoticworlds.engine.seam.VertexLoopWalk.mirrorPoint;
import static com.exoticworlds.engine.seam.VertexLoopWalk.serverPointAt;
import static com.exoticworlds.engine.seam.VertexLoopWalk.turnAfter;
import static com.exoticworlds.engine.seam.VertexLoopWalk.walkedAround;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.seam.ClientPosition;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

class ServerboundVertexChartTest {
    private static final int SECTION_Y = 4;
    private static final double BOX_SIZE_BLOCKS = 1.0;

    private record Sent(TranslationContext context, BlockPos serverBlock, BlockPos clientBlock, Vec3 serverPoint,
            Vec3 clientPoint) {
    }

    private static Sent sentAfterALoop() {
        ClientPosition mirror = walkedAround(HOP_ON_FRONT_AFTER_A_LOOP);
        Vec3 server = serverPointAt(HOP_ON_FRONT_AFTER_A_LOOP);
        Vec3 serverPoint = server.add(STEP_ON_THE_FACE);
        Vec3 clientPoint = mirrorPoint(mirror).add(turnAfter(HOP_ON_FRONT_AFTER_A_LOOP, STEP_ON_THE_FACE));
        BlockPos serverBlock = BlockPos.containing(serverPoint);
        Vec3 offset = Vec3.atCenterOf(serverBlock).subtract(server);
        Vec3 seated = mirrorPoint(mirror)
                .add(turnAfter(HOP_ON_FRONT_AFTER_A_LOOP, new Vec3(offset.x, 0.0, offset.z)));
        BlockPos clientBlock = new BlockPos(Mth.floor(seated.x), serverBlock.getY(), Mth.floor(seated.z));
        return new Sent(contextOf(mirror), serverBlock, clientBlock, serverPoint, clientPoint);
    }

    @Test
    void aBlockSentAfterALoopAroundAVertexLandsOnTheFaceOfThePathWithItsTurn() {
        Sent sent = sentAfterALoop();

        WorldFold.Folded<BlockPos> folded = sent.context().toServerOriented(sent.clientBlock());

        assertEquals(sent.serverBlock(), sent.context().toServer(sent.clientBlock()));
        assertEquals(sent.serverBlock(), folded.value());
        assertNear(EAST, folded.orientation().applyToDelta(turnAfter(HOP_ON_FRONT_AFTER_A_LOOP, EAST)), "turn");
    }

    @Test
    void aPointAndAChunkSentAfterALoopLandOnTheFaceOfThePath() {
        Sent sent = sentAfterALoop();

        assertNear(sent.serverPoint(), sent.context().toServer(sent.clientPoint()), "point");
        assertEquals(ChunkPos.containing(sent.serverBlock()),
                sent.context().toServer(ChunkPos.containing(sent.clientBlock())));
    }

    @Test
    void everyLeafOfAServerboundValueReadsThePathAfterALoop() {
        Sent sent = sentAfterALoop();
        TranslationContext context = sent.context();
        FoldedValue.Leaves leaves = FoldedValue.toServer(context);
        ChunkPos serverChunk = ChunkPos.containing(sent.serverBlock());
        ChunkPos clientChunk = ChunkPos.containing(sent.clientBlock());
        Vec3 clientPoint = sent.clientPoint();

        assertEquals(sent.serverBlock(), FoldedValue.walk(context, leaves, sent.clientBlock()));
        assertNear(sent.serverPoint(), (Vec3) FoldedValue.walk(context, leaves, clientPoint), "Vec3");
        assertEquals(serverChunk, FoldedValue.walk(context, leaves, clientChunk));
        assertEquals(SectionPos.of(serverChunk, SECTION_Y),
                FoldedValue.walk(context, leaves, SectionPos.of(clientChunk, SECTION_Y)));
        assertEquals(GlobalPos.of(Level.OVERWORLD, sent.serverBlock()),
                FoldedValue.walk(context, leaves, GlobalPos.of(Level.OVERWORLD, sent.clientBlock())));
        AABB box = (AABB) FoldedValue.walk(context, leaves,
                AABB.ofSize(clientPoint, BOX_SIZE_BLOCKS, BOX_SIZE_BLOCKS, BOX_SIZE_BLOCKS));
        assertNear(sent.serverPoint(), box.getCenter(), "AABB");
        Vector3dc vector = (Vector3dc) FoldedValue.walk(context, leaves,
                new Vector3d(clientPoint.x, clientPoint.y, clientPoint.z));
        assertNear(sent.serverPoint(), new Vec3(vector.x(), vector.y(), vector.z()), "Vector3dc");
    }
}
