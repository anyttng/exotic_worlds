package com.exoticworlds.engine.gen.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.SeamTransform;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

@Timeout(60)
class MovedStructureStartPiecesTest {
    private static final int X_SHIFT = 1024;
    private static final int Z_SHIFT = -512;

    private static final DeckTransformation CLOCKWISE =
            new DeckTransformation(new SeamTransform(-1, 1, X_SHIFT, Z_SHIFT, true));
    private static final DeckTransformation COUNTERCLOCKWISE =
            new DeckTransformation(new SeamTransform(1, -1, X_SHIFT, Z_SHIFT, true));
    private static final DeckTransformation LAP =
            new DeckTransformation(SeamTransform.translation(-X_SHIFT, X_SHIFT));

    @Test
    void aClockwiseTurnLaysThePieceBoxAlongTheOtherAxis() {
        StructurePiece piece = longPiece();

        MovedStructureStart.movePiece(piece, CLOCKWISE);

        assertEquals(new BoundingBox(996, 60, -412, 1003, 70, -381), piece.getBoundingBox(),
                "the box of a piece copied by a clockwise quarter turn is not the turned image of the original");
    }

    @Test
    void aCounterclockwiseTurnLaysThePieceBoxAlongTheOtherAxis() {
        StructurePiece piece = longPiece();

        MovedStructureStart.movePiece(piece, COUNTERCLOCKWISE);

        assertEquals(new BoundingBox(1044, 60, -644, 1051, 70, -613), piece.getBoundingBox(),
                "the box of a piece copied by a counterclockwise quarter turn is not the turned image of the original");
    }

    @Test
    void aTranslationShiftsThePieceBoxAndKeepsItsSizes() {
        StructurePiece piece = longPiece();

        MovedStructureStart.movePiece(piece, LAP);

        assertEquals(new BoundingBox(-924, 60, 1044, -893, 70, 1051), piece.getBoundingBox(),
                "the box of a piece copied by a translation is not the original shifted by the lap");
    }

    private static StructurePiece longPiece() {
        StructurePieceType type = (context, tag) -> null;
        return new StructurePiece(type, 0, new BoundingBox(100, 60, 20, 131, 70, 27)) {
            @Override
            protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            }

            @Override
            public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                    RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
            }
        };
    }
}
