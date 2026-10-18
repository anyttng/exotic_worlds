package com.exoticworlds.compat.supplementaries.mixin;

import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.gen.AddedStructureStarts;
import com.exoticworlds.engine.gen.SpreadRingScan;
import com.exoticworlds.mixin.StructureManagerAccessor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;

import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.supplementaries.common.worldgen.LocatedStructure;
import net.mehvahdjukaar.supplementaries.common.worldgen.StructureLocator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

@Mixin(StructureLocator.class)
public class StructureLocatorSeamMixin {
    @Unique
    private static final String FIND_NEAREST_STRUCTURES =
            "findNearestStructures(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/HolderSet;Lnet/minecraft/core/BlockPos;IZIIZ)Ljava/util/List;";

    @Unique
    private static final String LIST_ADD = "Ljava/util/List;add(Ljava/lang/Object;)Z";

    @WrapOperation(method = FIND_NEAREST_STRUCTURES, at = @At(value = "INVOKE", target = LIST_ADD),
            slice = @Slice(from = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/structure/placement/RandomSpreadStructurePlacement;getPotentialStructureChunk(JII)Lnet/minecraft/world/level/ChunkPos;")))
    private static boolean exotic_worlds$cellThroughTheSeam(List<Object> candidates, Object candidate,
            Operation<Boolean> original,
            @Local(argsOnly = true) ServerLevel level,
            @Local(argsOnly = true) BlockPos searchCenter,
            @Local Vec2i cell,
            @Local RandomSpreadStructurePlacement placement,
            @Local Holder<Structure> structure) {
        CarriedShape carried = ShapedChunkGenerator.carriedShapeOf(level.getChunkSource().getGenerator());
        if (carried == null) {
            return original.call(candidates, candidate);
        }

        Object placementInfo = ((Pair<?, ?>) candidate).getSecond();
        AddedStructureStarts added = AddedStructureStarts.of(level.getChunkSource().getGeneratorState(),
                ((StructureManagerAccessor) level.structureManager()).exotic_worlds$structureCheck(), carried);
        int spacing = placement.spacing();
        SpreadRingScan.of(carried.fold(), added, placement, level.getSeed(), new ChunkPos(searchCenter))
                .cell(Math.floorDiv(cell.x(), spacing), Math.floorDiv(cell.y(), spacing),
                        picked -> exotic_worlds$add(candidates, picked, placementInfo, original),
                        start -> start.structure().equals(structure)
                                ? exotic_worlds$add(candidates, start.chunk(), placementInfo, original)
                                : null);
        return true;
    }

    @WrapOperation(method = FIND_NEAREST_STRUCTURES, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/ChunkGenerator;getNearestGeneratedStructure(Ljava/util/Set;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/level/levelgen/structure/placement/ConcentricRingsStructurePlacement;)Lcom/mojang/datafixers/util/Pair;"))
    private static @Nullable Pair<BlockPos, Holder<Structure>> exotic_worlds$seatRingResult(
            ChunkGenerator generator, Set<Holder<Structure>> structures, ServerLevel level,
            StructureManager structureManager, BlockPos searchCenter, boolean skipKnown,
            ConcentricRingsStructurePlacement placement,
            Operation<@Nullable Pair<BlockPos, Holder<Structure>>> original) {
        Pair<BlockPos, Holder<Structure>> found = original.call(generator, structures, level, structureManager,
                searchCenter, skipKnown, placement);
        return found == null ? null : Pair.of(
                WorldLoopAttachments.transformerOf(level).nearestCopy(searchCenter, found.getFirst()),
                found.getSecond());
    }

    @WrapOperation(method = "flushCandidates", at = @At(value = "INVOKE", target = LIST_ADD))
    private static boolean exotic_worlds$seatResult(List<Object> found, Object result, Operation<Boolean> original,
            @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) BlockPos searchCenter) {
        LocatedStructure located = (LocatedStructure) result;
        BlockPos seated = WorldLoopAttachments.transformerOf(level).nearestCopy(searchCenter, located.position());
        LocatedStructure seatedResult = new LocatedStructure(seated, located.structure(), located.start(),
                searchCenter.distSqr(seated));
        return !found.contains(seatedResult) && original.call(found, seatedResult);
    }

    @Unique
    private static @Nullable Object exotic_worlds$add(List<Object> candidates, ChunkPos chunk, Object placementInfo,
            Operation<Boolean> original) {
        original.call(candidates, Pair.of(chunk, placementInfo));
        return null;
    }
}
