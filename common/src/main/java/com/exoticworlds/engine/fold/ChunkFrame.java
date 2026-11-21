package com.exoticworlds.engine.fold;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.FoldOrientation;
import com.exoticworlds.core.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

public final class ChunkFrame {
    public static WorldFold.@Nullable Folded<BlockPos> seat(WorldFold fold, ChunkPos own, BlockPos pos) {
        if (ChunkSeat.sitsIn(own, pos.getX(), pos.getZ()) || fold.preservesLocalIndices()) {
            return null;
        }

        WorldFold.Folded<BlockPos> folded = fold.foldOriented(pos);
        return ChunkSeat.sitsIn(own, folded.value().getX(), folded.value().getZ()) ? folded : null;
    }

    public static long seatColumn(WorldFold fold, ChunkPos own, int x, int z) {
        long raw = BlockPos.asLong(x, 0, z);
        if (ChunkSeat.sitsIn(own, x, z) || fold.preservesLocalIndices()) {
            return raw;
        }

        long folded = fold.foldBlockNode(raw);
        return ChunkSeat.sitsIn(own, BlockPos.getX(folded), BlockPos.getZ(folded)) ? folded : raw;
    }

    public static BlockState toReader(BlockState state, FoldOrientation orientation) {
        return orientation.inverse().applyToState(state);
    }

    public static BlockState toChunk(WorldFold fold, BlockPos pos, BlockState state) {
        return fold.preservesLocalIndices() ? state : fold.foldOriented(pos).orientation().applyToState(state);
    }

    private ChunkFrame() {
    }
}
