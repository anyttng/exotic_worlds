package com.exoticworlds.compat.eminus;

import java.util.function.IntUnaryOperator;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.AbstractLongSet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.world.level.ChunkPos;

public final class FoldedRegions extends AbstractLongSet {
    private static final int REGION_BITS = 5;

    private final LongSet listed;
    private final CellFold fold;
    private final int tileChunks;

    private FoldedRegions(LongSet listed, CellFold fold, int tileChunks) {
        this.listed = listed;
        this.fold = fold;
        this.tileChunks = tileChunks;
    }

    public static LongSet of(LongSet listed, CellFold fold, int tileChunks) {
        return fold.repeats() ? new FoldedRegions(listed, fold, tileChunks) : listed;
    }

    @Override
    public boolean contains(long region) {
        int minChunkX = ChunkPos.getX(region) * ChunkPos.REGION_SIZE;
        int minChunkZ = ChunkPos.getZ(region) * ChunkPos.REGION_SIZE;
        for (int dz = 0; dz < ChunkPos.REGION_SIZE; dz += tileChunks) {
            for (int dx = 0; dx < ChunkPos.REGION_SIZE; dx += tileChunks) {
                if (tileMeetsListed(minChunkX + dx, minChunkZ + dz)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean tileMeetsListed(int firstX, int firstZ) {
        IntArrayList zEnds = pieceEnds(firstZ, fold::foldChunkZ);
        for (int zPiece = 0; zPiece < zEnds.size(); zPiece += 2) {
            int pieceZ = zEnds.getInt(zPiece);
            IntArrayList xEnds = pieceEnds(firstX, x -> fold.foldChunkX(x, pieceZ));
            for (int xPiece = 0; xPiece < xEnds.size(); xPiece += 2) {
                for (int z : new int[] {pieceZ, zEnds.getInt(zPiece + 1)}) {
                    for (int x : new int[] {xEnds.getInt(xPiece), xEnds.getInt(xPiece + 1)}) {
                        int storedX = fold.foldChunkX(x, z);
                        int storedZ = fold.foldChunkZ(z);
                        if (listed.contains(ChunkPos.asLong(storedX >> REGION_BITS, storedZ >> REGION_BITS))) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private IntArrayList pieceEnds(int first, IntUnaryOperator folded) {
        IntArrayList ends = new IntArrayList();
        ends.add(first);
        int previous = folded.applyAsInt(first);
        for (int chunk = first + 1; chunk < first + tileChunks; chunk++) {
            int current = folded.applyAsInt(chunk);
            if (current != previous + 1) {
                ends.add(chunk - 1);
                ends.add(chunk);
            }

            previous = current;
        }

        ends.add(first + tileChunks - 1);
        return ends;
    }

    @Override
    public LongIterator iterator() {
        return listed.iterator();
    }

    @Override
    public int size() {
        return listed.size();
    }
}
