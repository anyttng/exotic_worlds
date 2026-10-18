package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.WorldFold;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public interface FramedStructureStart {
    @Nullable StructureStart exotic_worlds$framedBy(WorldGenLevel level, DeckTransformation move);

    default @Nullable StructureStart exotic_worlds$framedToward(WorldGenLevel level, WorldFold fold, ChunkPos center) {
        StructureStart self = (StructureStart) (Object) this;
        if (!self.isValid()) {
            return self;
        }

        ChunkPos startPos = self.getChunkPos();
        ChunkPos nearest = fold.nearestCopy(center, startPos);
        return exotic_worlds$framedBy(level, fold.deckTransformation(startPos, nearest));
    }
}
