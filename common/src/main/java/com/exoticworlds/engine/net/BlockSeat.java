package com.exoticworlds.engine.net;

import com.exoticworlds.core.fold.DeckTransformation;
import com.exoticworlds.core.fold.FoldOrientation;
import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

record BlockSeat(BlockPos pos, FoldOrientation orientation) {
    static BlockSeat of(TranslationContext context, BlockPos pos) {
        WorldFold fold = context.fold();
        if (fold.isPreservingLocalIndices()) {
            return new BlockSeat(context.toClient(pos), FoldOrientation.IDENTITY);
        }

        DeckTransformation move = context.toClientMove(ChunkPos.containing(pos));
        return new BlockSeat(move.apply(pos), move.getOrientation());
    }
}
