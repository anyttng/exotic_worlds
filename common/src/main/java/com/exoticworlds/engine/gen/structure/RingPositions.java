package com.exoticworlds.engine.gen.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.cuboid.CuboidStraightWalk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

public final class RingPositions {
    public static final int BIOME_SEARCH_RADIUS_BLOCKS = 112;

    public static final int RING_CHUNK_CENTER_BLOCKS = 8;

    private static final int BIOME_SEARCH_REACH_CHUNKS =
            SectionPos.blockToSectionCoord(BIOME_SEARCH_RADIUS_BLOCKS);

    private static final BlockPos RING_ORIGIN = new BlockPos(RING_CHUNK_CENTER_BLOCKS, 0, RING_CHUNK_CENTER_BLOCKS);

    public static Supplier<ChunkPos> searchOnTheWorld(WorldFold fold, int ringIndex, ChunkPos ringChunk,
            Supplier<ChunkPos> search) {
        if (!hasGroundAround(fold, ringIndex, ringChunk)) {
            return () -> ringChunk;
        }

        return () -> searchAround(fold, ringChunk, search);
    }

    private static boolean hasGroundAround(WorldFold fold, int ringIndex, ChunkPos ringChunk) {
        if (fold.getCuboidAtlas() != null) {
            return !fold.isOver(ringChunk);
        }

        return ringIndex == 0 || fold.chunkOvershoot(ringChunk) <= BIOME_SEARCH_REACH_CHUNKS;
    }

    private static ChunkPos searchAround(WorldFold fold, ChunkPos asking, Supplier<ChunkPos> search) {
        try (GenerationFoldContext.Context.FoldScope _ = GenerationFoldContext.getContext().withFold(fold, asking)) {
            return search.get();
        }
    }

    public static List<ChunkPos> worldsShare(WorldFold fold, List<ChunkPos> positions,
            UnaryOperator<ChunkPos> searchAt) {
        List<ChunkPos> inBounds = new ArrayList<>(positions.size());
        for (ChunkPos position : positions) {
            if (!fold.isOver(position)) {
                inBounds.add(position);
            }
        }

        if (inBounds.size() == positions.size()) {
            return positions;
        }

        if (!inBounds.isEmpty()) {
            return List.copyOf(inBounds);
        }

        return List.of(firstOnTheWorld(fold, positions.getFirst(), searchAt));
    }

    private static ChunkPos firstOnTheWorld(WorldFold fold, ChunkPos first, UnaryOperator<ChunkPos> searchAt) {
        CuboidAtlas atlas = fold.getCuboidAtlas();
        if (atlas == null) {
            return fold.fold(first);
        }

        ChunkPos carried = ChunkPos.containing(CuboidStraightWalk.of(atlas).landedBlock(RING_ORIGIN,
                SectionPos.sectionToBlockCoord(first.x(), RING_CHUNK_CENTER_BLOCKS),
                SectionPos.sectionToBlockCoord(first.z(), RING_CHUNK_CENTER_BLOCKS)));
        ChunkPos answer = searchAround(fold, carried, () -> searchAt.apply(carried));
        return ChunkPos.unpack(fold.foldChunkKeyFrom(carried.pack(), answer.pack()));
    }

    private RingPositions() {
    }
}
