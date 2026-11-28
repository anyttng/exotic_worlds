package com.exoticworlds.engine.fold;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.FoldOrientation;
import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

public final class ChunkFrame {
    public static WorldFold.@Nullable Folded<BlockPos> seat(WorldFold fold, ChunkPos own, BlockPos pos) {
        if (ChunkSeat.isSittingIn(own, pos.getX(), pos.getZ()) || fold.isPreservingLocalIndices()) {
            return null;
        }

        WorldFold.Folded<BlockPos> folded = fold.foldOriented(pos);
        return ChunkSeat.isSittingIn(own, folded.value().getX(), folded.value().getZ()) ? folded : null;
    }

    public static BlockPos seatPos(WorldFold fold, ChunkPos own, BlockPos pos) {
        WorldFold.Folded<BlockPos> seated = seat(fold, own, pos);
        return seated == null ? pos : seated.value();
    }

    public static long seatColumn(WorldFold fold, ChunkPos own, int x, int z) {
        long raw = BlockPos.asLong(x, 0, z);
        if (ChunkSeat.isSittingIn(own, x, z) || fold.isPreservingLocalIndices()) {
            return raw;
        }

        long folded = fold.foldBlockNode(raw);
        return ChunkSeat.isSittingIn(own, BlockPos.getX(folded), BlockPos.getZ(folded)) ? folded : raw;
    }

    public static BlockState toReader(BlockState state, FoldOrientation orientation) {
        return orientation.inverse().applyToState(state);
    }

    public static BlockState toChunk(WorldFold fold, BlockPos pos, BlockState state) {
        return fold.isPreservingLocalIndices() ? state : toChunk(state, fold.foldOriented(pos).orientation());
    }

    public static BlockState toChunk(BlockState state, FoldOrientation orientation) {
        return orientation.applyToState(state);
    }

    private ChunkFrame() {
    }
}
