package com.exoticworlds.mixin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.accessors.FramedStructureStart;
import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;

@Mixin(StructureStart.class)
public class StructureStartMixin implements FramedStructureStart {
    @Unique
    private volatile @Nullable Map<DeckTransformation, StructureStart> toroidal$framed;

    @Override
    public @Nullable StructureStart toroidal$framedBy(WorldGenLevel level, DeckTransformation move) {
        StructureStart self = (StructureStart) (Object) this;
        if (move.isIdentity()) {
            return self;
        }

        Map<DeckTransformation, StructureStart> framed = this.toroidal$cache();
        StructureStart known = framed.get(move);
        if (known != null) {
            return known;
        }

        StructureStart made = toroidal$movedCopy(self, level, move);
        if (made == null) {
            return null;
        }

        StructureStart raced = framed.putIfAbsent(move, made);
        return raced != null ? raced : made;
    }

    @WrapMethod(method = "placeInChunk")
    private void toroidal$placeInTheStartsFrame(WorldGenLevel level, StructureManager structureManager,
            ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos,
            Operation<Void> original) {
        List<StructurePiece> pieces = ((StructureStart) (Object) this).getPieces();
        if (pieces.isEmpty()) {
            original.call(level, structureManager, generator, random, chunkBB, chunkPos);
            return;
        }

        BlockPos reference = pieces.getFirst().getBoundingBox().getCenter();
        DeckTransformation move = WorldLoopAttachments.transformerOf(level.getLevel())
                .nearestCopyTransformation(reference, chunkPos.getMiddleBlockPosition(reference.getY()));
        if (move.isIdentity()) {
            original.call(level, structureManager, generator, random, chunkBB, chunkPos);
            return;
        }

        original.call(level, structureManager, generator, random, move.apply(chunkBB),
                new ChunkPos(move.apply(chunkPos.getWorldPosition())));
    }

    @Unique
    private Map<DeckTransformation, StructureStart> toroidal$cache() {
        Map<DeckTransformation, StructureStart> framed = this.toroidal$framed;
        if (framed != null) {
            return framed;
        }

        synchronized (this) {
            framed = this.toroidal$framed;
            if (framed == null) {
                framed = new ConcurrentHashMap<>(2);
                this.toroidal$framed = framed;
            }

            return framed;
        }
    }

    @Unique
    private static @Nullable StructureStart toroidal$movedCopy(
            StructureStart start, WorldGenLevel level, DeckTransformation move) {
        StructurePieceSerializationContext context = StructurePieceSerializationContext.fromLevel(level.getLevel());
        StructureStart copy = StructureStart.loadStaticStart(
                context, start.createTag(context, start.getChunkPos()), level.getSeed());
        if (copy == null || !copy.isValid()) {
            return null;
        }

        copy.getPieces().forEach(piece -> {
            BoundingBox box = piece.getBoundingBox();
            BoundingBox moved = move.apply(box);
            piece.move(moved.minX() - box.minX(), 0, moved.minZ() - box.minZ());

            if (piece instanceof PoolElementStructurePiece poolPiece) {
                poolPiece.getJunctions().replaceAll(junction -> {
                    BlockPos source = move.apply(new BlockPos(
                            junction.getSourceX(), junction.getSourceGroundY(), junction.getSourceZ()));
                    return new JigsawJunction(
                            source.getX(),
                            junction.getSourceGroundY(),
                            source.getZ(),
                            junction.getDeltaY(),
                            junction.getDestProjection());
                });
            }
        });
        return copy;
    }
}
