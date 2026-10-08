package com.exoticworlds.engine.gen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.accessors.AddedStartsHolder;
import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.api.v1.gen.StructureStarts;
import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.GenerationMoments;
import com.exoticworlds.core.ToroidalShapeView;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;
import com.exoticworlds.mixin.StructureCheckAccessor;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

public final class AddedStructureStarts {
    public static final AddedStructureStarts NONE = new AddedStructureStarts(new Long2ObjectOpenHashMap<>(), Map.of());

    private final Long2ObjectMap<List<StructureSet.StructureSelectionEntry>> byChunk;
    private final Map<StructurePlacement, List<StructureStarts.Added>> byPlacement;

    private AddedStructureStarts(Long2ObjectMap<List<StructureSet.StructureSelectionEntry>> byChunk,
            Map<StructurePlacement, List<StructureStarts.Added>> byPlacement) {
        this.byChunk = byChunk;
        this.byPlacement = byPlacement;
    }

    public static AddedStructureStarts of(ChunkGeneratorStructureState state, StructureCheck check,
            CarriedShape carried) {
        AddedStartsHolder holder = (AddedStartsHolder) (Object) state;
        AddedStructureStarts known = holder.toroidal$addedStarts();
        if (known != null) {
            return known;
        }

        synchronized (holder) {
            known = holder.toroidal$addedStarts();
            if (known == null) {
                known = GenerationMoments.hasStructureStartHooks() ? compute(state, check, carried) : NONE;
                holder.toroidal$addedStarts(known);
            }

            return known;
        }
    }

    public List<StructureSet.StructureSelectionEntry> at(ChunkPos chunk) {
        List<StructureSet.StructureSelectionEntry> placed = this.byChunk.get(chunk.pack());
        return placed != null ? placed : List.of();
    }

    public List<StructureStarts.Added> of(StructurePlacement placement) {
        return this.byPlacement.getOrDefault(placement, List.of());
    }

    private static AddedStructureStarts compute(ChunkGeneratorStructureState state, StructureCheck check,
            CarriedShape carried) {
        WorldFold fold = carried.fold();
        ToroidalShape shape = new ToroidalShapeView(fold);
        Long2ObjectMap<List<StructureSet.StructureSelectionEntry>> byChunk = new Long2ObjectOpenHashMap<>();
        Map<StructurePlacement, List<StructureStarts.Added>> byPlacement = new HashMap<>();
        for (Holder<StructureSet> set : state.possibleStructureSets()) {
            SetView view = new SetView(state, check, fold, shape, carried.generationOptions(), set);
            List<StructureStarts.Added> accepted = view.accepted(GenerationMoments.runAtStructureStarts(view));
            if (accepted.isEmpty()) {
                continue;
            }

            byPlacement.computeIfAbsent(set.value().placement(), placement -> new ArrayList<>()).addAll(accepted);
            for (StructureStarts.Added added : accepted) {
                byChunk.computeIfAbsent(added.chunk().pack(), key -> new ArrayList<>())
                        .add(view.entryOf(added.structure()).orElseThrow());
            }
        }

        return new AddedStructureStarts(byChunk, byPlacement);
    }

    private static final class SetView implements StructureStarts {
        private final ChunkGeneratorStructureState state;
        private final StructureCheck check;
        private final WorldFold fold;
        private final ToroidalShape shape;
        private final GenerationOptions options;
        private final Holder<StructureSet> set;
        private @Nullable List<Pick> picks;

        private SetView(ChunkGeneratorStructureState state, StructureCheck check, WorldFold fold, ToroidalShape shape,
                GenerationOptions options, Holder<StructureSet> set) {
            this.state = state;
            this.check = check;
            this.fold = fold;
            this.shape = shape;
            this.options = options;
            this.set = set;
        }

        @Override
        public ToroidalShape shape() {
            return this.shape;
        }

        @Override
        public GenerationOptions options() {
            return this.options;
        }

        @Override
        public Holder<StructureSet> structureSet() {
            return this.set;
        }

        @Override
        public List<Pick> picks() {
            if (this.picks == null) {
                this.picks = enumeratePicks();
            }

            return this.picks;
        }

        @Override
        public boolean canStart(Holder<Structure> structure, ChunkPos chunk) {
            return ((StructureCheckAccessor) this.check).toroidal$canCreateStructure(chunk, structure.value());
        }

        private List<Pick> enumeratePicks() {
            WrapDomain xDomain = this.fold.chunkDomain(Direction.Axis.X);
            WrapDomain zDomain = this.fold.chunkDomain(Direction.Axis.Z);
            if (!xDomain.loops() || !zDomain.loops()) {
                throw new IllegalStateException("Picks are enumerated only where both horizontal axes loop");
            }

            if (!(this.set.value().placement() instanceof RandomSpreadStructurePlacement spread)) {
                return List.of();
            }

            int spacing = spread.spacing();
            long seed = this.state.getLevelSeed();
            List<Pick> found = new ArrayList<>();
            for (int cellX = Math.floorDiv(xDomain.lowerBound, spacing);
                    cellX <= Math.floorDiv(xDomain.upperBound - 1, spacing); cellX++) {
                for (int cellZ = Math.floorDiv(zDomain.lowerBound, spacing);
                        cellZ <= Math.floorDiv(zDomain.upperBound - 1, spacing); cellZ++) {
                    ChunkPos chunk = spread.getPotentialStructureChunk(seed, cellX * spacing, cellZ * spacing);
                    if (this.fold.isOver(chunk) || !spread.isStructureChunk(this.state, chunk.x(), chunk.z())) {
                        continue;
                    }

                    found.add(new Pick(chunk, selected(seed, chunk)));
                }
            }

            return List.copyOf(found);
        }

        private Optional<Holder<Structure>> selected(long seed, ChunkPos chunk) {
            List<StructureSet.StructureSelectionEntry> options = new ArrayList<>(this.set.value().structures());
            WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
            random.setLargeFeatureSeed(seed, chunk.x(), chunk.z());
            int total = 0;
            for (StructureSet.StructureSelectionEntry option : options) {
                total += option.weight();
            }

            while (!options.isEmpty()) {
                int choice = random.nextInt(total);
                int index = 0;
                for (StructureSet.StructureSelectionEntry option : options) {
                    choice -= option.weight();
                    if (choice < 0) {
                        break;
                    }

                    index++;
                }

                StructureSet.StructureSelectionEntry chosen = options.get(index);
                if (canStart(chosen.structure(), chunk)) {
                    return Optional.of(chosen.structure());
                }

                options.remove(index);
                total -= chosen.weight();
            }

            return Optional.empty();
        }

        private Optional<StructureSet.StructureSelectionEntry> entryOf(Holder<Structure> structure) {
            return this.set.value().structures().stream()
                    .filter(entry -> entry.structure().equals(structure))
                    .findFirst();
        }

        private List<StructureStarts.Added> accepted(List<StructureStarts.Added> proposed) {
            if (proposed.isEmpty()) {
                return List.of();
            }

            Set<ChunkPos> picked = new HashSet<>();
            for (Pick pick : picks()) {
                picked.add(pick.chunk());
            }

            Set<StructureStarts.Added> seen = new HashSet<>();
            List<StructureStarts.Added> accepted = new ArrayList<>();
            for (StructureStarts.Added added : proposed) {
                String refusal = refusalOf(added, picked);
                if (refusal != null) {
                    ExoticWorlds.LOGGER.warn("[structure-starts] dropped structure={} chunk={} reason={}",
                            added.structure().getRegisteredName(), added.chunk(), refusal);
                } else if (seen.add(added)) {
                    accepted.add(added);
                }
            }

            return List.copyOf(accepted);
        }

        private @Nullable String refusalOf(StructureStarts.Added added, Set<ChunkPos> picked) {
            if (this.fold.isOver(added.chunk())) {
                return "outside_bounds";
            }

            if (picked.contains(added.chunk())) {
                return "picked_chunk";
            }

            return entryOf(added.structure()).isPresent() ? null : "not_in_set";
        }
    }
}
