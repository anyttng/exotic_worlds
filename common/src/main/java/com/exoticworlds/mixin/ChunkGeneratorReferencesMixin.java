package com.exoticworlds.mixin;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.WorldFold;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.StructureAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorReferencesMixin {
    @Unique
    private static final String SOURCE_POS = "sourcePos";

    @WrapOperation(
            method = "createReferences",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getAllStarts()Ljava/util/Map;"))
    private Map<Structure, StructureStart> exotic_worlds$startsOfNearestCopy(ChunkAccess sourceChunk,
            Operation<Map<Structure, StructureStart>> original, @Local(argsOnly = true) ChunkAccess centerChunk,
            @Local(name = "sourceX") int sourceX, @Local(name = "sourceZ") int sourceZ,
            @Share(SOURCE_POS) LocalRef<@Nullable ChunkPos> sourcePos) {
        WorldFold transformer = ShapedChunkGenerator.wrappedTransformerOf((ChunkGenerator) (Object) this);
        if (transformer == null) {
            return original.call(sourceChunk);
        }

        ChunkPos source = new ChunkPos(sourceX, sourceZ);
        if (!transformer.nearestCopy(centerChunk.getPos(), transformer.fold(source)).equals(source)) {
            return Map.of();
        }

        sourcePos.set(sourceChunk.getPos());
        return original.call(sourceChunk);
    }

    @WrapOperation(
            method = "createReferences",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/structure/BoundingBox;intersects(IIII)Z"))
    private boolean exotic_worlds$intersectsInSourceFrame(BoundingBox box, int minX, int minZ, int maxX, int maxZ,
            Operation<Boolean> original, @Local(argsOnly = true) ChunkAccess centerChunk,
            @Local(name = "sourceX") int sourceX, @Local(name = "sourceZ") int sourceZ,
            @Share(SOURCE_POS) LocalRef<@Nullable ChunkPos> sourcePos) {
        WorldFold transformer = ShapedChunkGenerator.wrappedTransformerOf((ChunkGenerator) (Object) this);
        ChunkPos physical = sourcePos.get();
        if (transformer == null || physical == null) {
            return original.call(box, minX, minZ, maxX, maxZ);
        }

        ChunkPos foldedCenter = transformer.deckTransformation(new ChunkPos(sourceX, sourceZ), physical)
                .apply(centerChunk.getPos());
        return original.call(box, foldedCenter.getMinBlockX(), foldedCenter.getMinBlockZ(),
                foldedCenter.getMaxBlockX(), foldedCenter.getMaxBlockZ());
    }

    @WrapOperation(
            method = "createReferences",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/StructureManager;addReferenceForStructure("
                            + "Lnet/minecraft/core/SectionPos;Lnet/minecraft/world/level/levelgen/structure/Structure;J"
                            + "Lnet/minecraft/world/level/chunk/StructureAccess;)V"))
    private void exotic_worlds$referencePhysicalSource(StructureManager structureManager, SectionPos centerSection,
            Structure structure, long reference, StructureAccess centerChunk, Operation<Void> original,
            @Share(SOURCE_POS) LocalRef<@Nullable ChunkPos> sourcePos) {
        ChunkPos physical = sourcePos.get();
        original.call(structureManager, centerSection, structure, physical == null ? reference : physical.pack(),
                centerChunk);
    }
}
