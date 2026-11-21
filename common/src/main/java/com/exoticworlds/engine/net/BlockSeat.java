package com.exoticworlds.engine.net;

import com.exoticworlds.core.FoldOrientation;
import com.exoticworlds.core.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

record BlockSeat(BlockPos pos, FoldOrientation orientation) {
    static BlockSeat of(TranslationContext context, BlockPos pos) {
        WorldFold fold = context.fold();
        if (fold.preservesLocalIndices()) {
            return new BlockSeat(context.toClient(pos), FoldOrientation.IDENTITY);
        }

        ChunkPos serverChunk = ChunkPos.containing(pos);
        ChunkPos clientChunk = context.toClient(serverChunk);
        return new BlockSeat(fold.reseat(pos, clientChunk),
                fold.deckTransformation(serverChunk, clientChunk).orientation());
    }
}
