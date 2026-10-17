package com.exoticworlds.mixin;

import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.gen.AddedStructureStarts;
import com.exoticworlds.engine.gen.SpreadRingScan;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorRandomSpreadSearchMixin {
    @Unique
    private static final String NEAREST_GENERATED_STRUCTURE =
            "Lnet/minecraft/world/level/chunk/ChunkGenerator;getNearestGeneratedStructure(Ljava/util/Set;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/world/level/StructureManager;IIIZJLnet/minecraft/world/level/levelgen/structure/placement/RandomSpreadStructurePlacement;)Lcom/mojang/datafixers/util/Pair;";

    @WrapOperation(method = "findNearestMapStructure", at = @At(value = "INVOKE", target = NEAREST_GENERATED_STRUCTURE))
    private @Nullable Pair<BlockPos, Holder<Structure>> toroidal$ringsThroughTheSeam(
            Set<Holder<Structure>> structures,
            LevelReader level,
            StructureManager structureManager,
            int chunkOriginX,
            int chunkOriginZ,
            int radius,
            boolean createReference,
            long seed,
            RandomSpreadStructurePlacement placement,
            Operation<@Nullable Pair<BlockPos, Holder<Structure>>> original,
            @Local(argsOnly = true) ServerLevel serverLevel) {
        CarriedShape carried = ShapedChunkGenerator.carriedShapeOf((ChunkGenerator) (Object) this);
        if (carried == null) {
            return original.call(structures, level, structureManager, chunkOriginX, chunkOriginZ, radius,
                    createReference, seed, placement);
        }

        AddedStructureStarts added = AddedStructureStarts.of(serverLevel.getChunkSource().getGeneratorState(),
                ((StructureManagerAccessor) structureManager).toroidal$structureCheck(), carried);
        return SpreadRingScan.of(carried.fold(), added, placement, seed, new ChunkPos(chunkOriginX, chunkOriginZ))
                .ring(radius,
                        candidate -> ChunkGeneratorAccessor.toroidal$structureGeneratingAt(
                                structures, level, structureManager, createReference, placement, candidate),
                        start -> structures.contains(start.structure())
                                ? ChunkGeneratorAccessor.toroidal$structureGeneratingAt(Set.of(start.structure()),
                                        level, structureManager, createReference, placement, start.chunk())
                                : null);
    }

    @WrapOperation(
            method = "findNearestMapStructure",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_DIST_SQR))
    private double toroidal$rankThroughTheSeam(BlockPos origin, Vec3i candidate, Operation<Double> original) {
        WorldFold transformer = ShapedChunkGenerator.wrappedTransformerOf((ChunkGenerator) (Object) this);
        if (transformer == null) {
            return original.call(origin, candidate);
        }

        return SeamRange.sqr(transformer, origin, candidate);
    }
}
