package com.exoticworlds.engine.gen.structure;

import java.util.List;

import net.minecraft.world.level.ChunkPos;

sealed interface SectorCells permits LatticeSectorCells, SplitSectorCells {
    record Read(int cellX, int cellZ, int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        static Read whole(int cellX, int cellZ) {
            return new Read(cellX, cellZ, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        boolean holds(ChunkPos chunk) {
            return chunk.x() >= this.minChunkX && chunk.x() <= this.maxChunkX
                    && chunk.z() >= this.minChunkZ && chunk.z() <= this.maxChunkZ;
        }
    }

    int offsetCap();

    List<Read> reads(int x, int z);
}
