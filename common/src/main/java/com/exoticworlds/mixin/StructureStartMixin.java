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
import com.exoticworlds.engine.gen.MovedStructureStart;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

@Mixin(StructureStart.class)
public class StructureStartMixin implements FramedStructureStart {
    @Unique
    private volatile @Nullable Map<DeckTransformation, StructureStart> exotic_worlds$framed;

    @Override
    public @Nullable StructureStart exotic_worlds$framedBy(WorldGenLevel level, DeckTransformation move) {
        StructureStart self = (StructureStart) (Object) this;
        if (move.isIdentity()) {
            return self;
        }

        Map<DeckTransformation, StructureStart> framed = this.exotic_worlds$cache();
        StructureStart known = framed.get(move);
        if (known != null) {
            return known;
        }

        StructureStart made = MovedStructureStart.copy(self, level, move);
        if (made == null) {
            return null;
        }

        StructureStart raced = framed.putIfAbsent(move, made);
        return raced != null ? raced : made;
    }

    @WrapMethod(method = "placeInChunk")
    private void exotic_worlds$placeInTheStartsFrame(WorldGenLevel level, StructureManager structureManager,
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
                ChunkPos.containing(move.apply(chunkPos.getWorldPosition())));
    }

    @Unique
    private Map<DeckTransformation, StructureStart> exotic_worlds$cache() {
        Map<DeckTransformation, StructureStart> framed = this.exotic_worlds$framed;
        if (framed != null) {
            return framed;
        }

        synchronized (this) {
            framed = this.exotic_worlds$framed;
            if (framed == null) {
                framed = new ConcurrentHashMap<>(2);
                this.exotic_worlds$framed = framed;
            }

            return framed;
        }
    }
}
