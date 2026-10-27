package com.exoticworlds.engine.gen;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.DeckTransformation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;

public final class MovedStructureStart {
    public static @Nullable StructureStart copy(StructureStart start, WorldGenLevel level, DeckTransformation move) {
        StructurePieceSerializationContext context = StructurePieceSerializationContext.fromLevel(level.getLevel());
        StructureStart copy = StructureStart.loadStaticStart(
                context, start.createTag(context, start.getChunkPos()), level.getSeed());
        if (copy == null || !copy.isValid()) {
            return null;
        }

        movePieces(copy, move);
        return copy;
    }

    private static void movePieces(StructureStart start, DeckTransformation move) {
        start.getPieces().forEach(piece -> {
            BoundingBox box = piece.getBoundingBox();
            BoundingBox moved = move.apply(box);
            piece.move(moved.minX() - box.minX(), 0, moved.minZ() - box.minZ());

            if (piece instanceof PoolElementStructurePiece poolPiece) {
                poolPiece.getJunctions().replaceAll(junction -> movedJunction(junction, move));
            }
        });
    }

    public static JigsawJunction movedJunction(JigsawJunction junction, DeckTransformation move) {
        BlockPos source = move.apply(new BlockPos(
                junction.getSourceX(), junction.getSourceGroundY(), junction.getSourceZ()));
        return new JigsawJunction(
                source.getX(),
                junction.getSourceGroundY(),
                source.getZ(),
                junction.getDeltaY(),
                junction.getDestProjection());
    }

    private MovedStructureStart() {
    }
}
