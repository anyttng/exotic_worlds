package com.exoticworlds.shape.torus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.api.v1.gen.GenerationHooks;
import com.exoticworlds.api.v1.gen.StructureStarts;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.phys.Vec3;

public final class NetherComplexStarts {
    private static final ChunkPos WORLD_ORIGIN = new ChunkPos(0, 0);

    public static void register() {
        GenerationHooks.atStructureStarts(GuaranteedNetherComplexes.KEY, NetherComplexStarts::starts);
    }

    private static List<StructureStarts.Added> starts(StructureStarts set) {
        if (!set.options().get(GuaranteedNetherComplexes.OPTION)
                || !set.structureSet().is(BuiltinStructureSets.NETHER_COMPLEXES)) {
            return List.of();
        }

        ToroidalShape shape = set.shape();
        StructureSet structures = set.structureSet().value();
        if (!shape.loops(Direction.Axis.X) || !shape.loops(Direction.Axis.Z)
                || !(structures.placement() instanceof RandomSpreadStructurePlacement spread)) {
            return List.of();
        }

        List<ChunkPos> taken = new ArrayList<>();
        Set<Holder<Structure>> present = new HashSet<>();
        for (StructureStarts.Pick pick : set.picks()) {
            taken.add(pick.chunk());
            pick.structure().ifPresent(present::add);
        }

        List<StructureStarts.Added> added = new ArrayList<>();
        for (StructureSet.StructureSelectionEntry entry : structures.structures()) {
            Holder<Structure> structure = entry.structure();
            if (present.contains(structure)) {
                continue;
            }

            ChunkPos chosen = firstFree(shape, chunk -> set.canStart(structure, chunk), taken, spread.separation());
            if (chosen == null) {
                ExoticWorlds.LOGGER.warn("[guaranteed-nether-complexes] no chunk in bounds takes structure={}",
                        structure.getRegisteredName());
                continue;
            }

            added.add(new StructureStarts.Added(structure, chosen));
            taken.add(chosen);
        }

        return added;
    }

    static @Nullable ChunkPos firstFree(ToroidalShape shape, Predicate<ChunkPos> canStart, List<ChunkPos> taken,
            int separation) {
        int xWidth = shape.widthChunks(Direction.Axis.X);
        int zWidth = shape.widthChunks(Direction.Axis.Z);
        ChunkPos origin = shape.fold(WORLD_ORIGIN);
        Set<ChunkPos> visited = new HashSet<>();
        int farthestRing = Math.max(xWidth, zWidth) / 2;
        for (int ring = 0; ring <= farthestRing; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }

                    ChunkPos chunk = shape.fold(new ChunkPos(origin.x + dx, origin.z + dz));
                    if (!visited.add(chunk) || isNearAny(shape, chunk, taken, separation)) {
                        continue;
                    }

                    if (canStart.test(chunk)) {
                        return chunk;
                    }
                }
            }
        }

        return null;
    }

    private static boolean isNearAny(ToroidalShape shape, ChunkPos chunk, List<ChunkPos> taken, int separation) {
        for (ChunkPos other : taken) {
            Vec3 delta = shape.shortestDelta(cornerOf(chunk), cornerOf(other));
            if (Math.max(Math.abs(delta.x), Math.abs(delta.z)) < separation * SectionPos.SECTION_SIZE) {
                return true;
            }
        }

        return false;
    }

    private static Vec3 cornerOf(ChunkPos chunk) {
        return new Vec3(chunk.getMinBlockX(), 0.0, chunk.getMinBlockZ());
    }

    private NetherComplexStarts() {
    }
}
