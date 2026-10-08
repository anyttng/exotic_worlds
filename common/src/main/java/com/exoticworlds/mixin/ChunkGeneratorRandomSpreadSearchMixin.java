package com.exoticworlds.mixin;

import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.api.v1.gen.StructureStarts;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.gen.AddedStructureStarts;
import com.exoticworlds.engine.gen.SectorGridAxis;
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

        WorldFold transformer = carried.fold();
        List<StructureStarts.Added> added = AddedStructureStarts.of(serverLevel.getChunkSource().getGeneratorState(),
                ((StructureManagerAccessor) structureManager).toroidal$structureCheck(), carried).of(placement);

        TranslationLattice lattice = transformer.chunkLattice();
        SectorGridAxis xCells = SectorGridAxis.of(lattice.x(), placement.spacing(), chunkOriginX);
        SectorGridAxis zCells = SectorGridAxis.of(lattice.z(), placement.spacing(), chunkOriginZ);
        if (radius > Math.max(xCells.offsetCap(), zCells.offsetCap())) {
            return null;
        }

        for (int x = -radius; x <= radius; x++) {
            if (Math.abs(x) > xCells.offsetCap()) {
                continue;
            }

            boolean xEdge = x == -radius || x == radius;
            for (int z = -radius; z <= radius; z++) {
                if (!xEdge && z != -radius && z != radius) {
                    continue;
                }

                if (Math.abs(z) > zCells.offsetCap()) {
                    continue;
                }

                int probeX = xCells.probeChunkShifted(x, -zCells.lapsCrossed(z) * lattice.skew());
                int probeZ = zCells.probeChunk(z);
                ChunkPos candidate = placement.getPotentialStructureChunk(seed, probeX, probeZ);
                if (!transformer.isOver(candidate)) {
                    Pair<BlockPos, Holder<Structure>> generating =
                            ChunkGeneratorAccessor.toroidal$structureGeneratingAt(
                                    structures, level, structureManager, createReference, placement, candidate);
                    if (generating != null) {
                        return generating;
                    }
                }

                Pair<BlockPos, Holder<Structure>> addedGenerating = toroidal$addedStartIn(added, structures, level,
                        structureManager, createReference, placement, probeX, probeZ);
                if (addedGenerating != null) {
                    return addedGenerating;
                }
            }
        }

        return null;
    }

    @Unique
    private static @Nullable Pair<BlockPos, Holder<Structure>> toroidal$addedStartIn(List<StructureStarts.Added> added,
            Set<Holder<Structure>> structures, LevelReader level, StructureManager structureManager,
            boolean createReference, RandomSpreadStructurePlacement placement, int probeX, int probeZ) {
        int spacing = placement.spacing();
        for (StructureStarts.Added start : added) {
            if (!structures.contains(start.structure())
                    || Math.floorDiv(start.chunk().x, spacing) != Math.floorDiv(probeX, spacing)
                    || Math.floorDiv(start.chunk().z, spacing) != Math.floorDiv(probeZ, spacing)) {
                continue;
            }

            Pair<BlockPos, Holder<Structure>> generating = ChunkGeneratorAccessor.toroidal$structureGeneratingAt(
                    Set.of(start.structure()), level, structureManager, createReference, placement, start.chunk());
            if (generating != null) {
                return generating;
            }
        }

        return null;
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
