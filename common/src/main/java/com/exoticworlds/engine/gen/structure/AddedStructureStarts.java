package com.exoticworlds.engine.gen.structure;

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
import com.exoticworlds.core.feature.ShapeFeatures;
import com.exoticworlds.core.feature.StructureStartsFeature;
import com.exoticworlds.core.gen.AddedStart;
import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.gen.GenerationMoments;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.gen.StructurePick;
import com.exoticworlds.core.gen.StructureStartSet;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WrapDomain;
import com.exoticworlds.mixin.StructureCheckAccessor;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
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
    private final Map<StructurePlacement, List<AddedStart>> byPlacement;

    private AddedStructureStarts(Long2ObjectMap<List<StructureSet.StructureSelectionEntry>> byChunk,
            Map<StructurePlacement, List<AddedStart>> byPlacement) {
        this.byChunk = byChunk;
        this.byPlacement = byPlacement;
    }

    public static AddedStructureStarts of(ChunkGeneratorStructureState state, StructureCheck check,
            CarriedShape carried, ResourceKey<Level> level) {
        AddedStartsHolder holder = (AddedStartsHolder) (Object) state;
        AddedStructureStarts known = holder.exotic_worlds$getAddedStarts();
        if (known != null) {
            return known;
        }

        synchronized (holder) {
            known = holder.exotic_worlds$getAddedStarts();
            if (known == null) {
                List<StructureStartsFeature> features = ShapeFeatures.activeAt(carried,
                        Registries.levelToLevelStem(level), StructureStartsFeature.class);
                known = features.isEmpty() && !GenerationMoments.hasStructureStartHooks()
                        ? NONE
                        : compute(state, check, carried, features);
                holder.exotic_worlds$setAddedStarts(known);
            }

            return known;
        }
    }

    private static List<AddedStart> startsIn(StructureStartSet view, List<StructureStartsFeature> features) {
        List<AddedStart> starts = new ArrayList<>();
        for (StructureStartsFeature feature : features) {
            starts.addAll(feature.startsIn(view));
        }
        starts.addAll(GenerationMoments.runAtStructureStarts(view));
        return starts;
    }

    public List<StructureSet.StructureSelectionEntry> getEntriesAt(ChunkPos chunk) {
        List<StructureSet.StructureSelectionEntry> placed = this.byChunk.get(chunk.pack());
        return placed != null ? placed : List.of();
    }

    public List<AddedStart> of(StructurePlacement placement) {
        return this.byPlacement.getOrDefault(placement, List.of());
    }

    private static AddedStructureStarts compute(ChunkGeneratorStructureState state, StructureCheck check,
            CarriedShape carried, List<StructureStartsFeature> features) {
        WorldFold fold = carried.getFold();
        Long2ObjectMap<List<StructureSet.StructureSelectionEntry>> byChunk = new Long2ObjectOpenHashMap<>();
        Map<StructurePlacement, List<AddedStart>> byPlacement = new HashMap<>();
        for (Holder<StructureSet> set : state.possibleStructureSets()) {
            SetView view = new SetView(state, check, fold, carried.getOptions(), set);
            List<AddedStart> accepted = view.accepted(startsIn(view, features));
            if (accepted.isEmpty()) {
                continue;
            }

            byPlacement.computeIfAbsent(set.value().placement(), placement -> new ArrayList<>()).addAll(accepted);
            for (AddedStart added : accepted) {
                byChunk.computeIfAbsent(added.chunk().pack(), key -> new ArrayList<>())
                        .add(view.getEntry(added.structure()).orElseThrow());
            }
        }

        return new AddedStructureStarts(byChunk, byPlacement);
    }

    private static final class SetView implements StructureStartSet {
        private final ChunkGeneratorStructureState state;
        private final StructureCheck check;
        private final WorldFold fold;
        private final ShapeOptions options;
        private final Holder<StructureSet> set;
        private @Nullable List<StructurePick> picks;

        private SetView(ChunkGeneratorStructureState state, StructureCheck check, WorldFold fold,
                ShapeOptions options, Holder<StructureSet> set) {
            this.state = state;
            this.check = check;
            this.fold = fold;
            this.options = options;
            this.set = set;
        }

        @Override
        public WorldFold getFold() {
            return this.fold;
        }

        @Override
        public ShapeOptions getOptions() {
            return this.options;
        }

        @Override
        public Holder<StructureSet> getStructureSet() {
            return this.set;
        }

        @Override
        public List<StructurePick> getPicks() {
            if (this.picks == null) {
                this.picks = enumeratePicks();
            }

            return this.picks;
        }

        @Override
        public boolean canStart(Holder<Structure> structure, ChunkPos chunk) {
            return ((StructureCheckAccessor) this.check).exotic_worlds$canCreateStructure(chunk, structure.value());
        }

        private List<StructurePick> enumeratePicks() {
            WrapDomain xDomain = this.fold.getChunkLattice().getX();
            WrapDomain zDomain = this.fold.getChunkLattice().getZ();
            if (!xDomain.isLooped() || !zDomain.isLooped()) {
                throw new IllegalStateException("Picks are enumerated only where both horizontal axes loop");
            }

            if (!(this.set.value().placement() instanceof RandomSpreadStructurePlacement spread)) {
                return List.of();
            }

            int spacingChunks = spread.spacing();
            long seed = this.state.getLevelSeed();
            List<StructurePick> found = new ArrayList<>();
            for (int cellX = Math.floorDiv(xDomain.lowerBound, spacingChunks);
                    cellX <= Math.floorDiv(xDomain.upperBound - 1, spacingChunks); cellX++) {
                for (int cellZ = Math.floorDiv(zDomain.lowerBound, spacingChunks);
                        cellZ <= Math.floorDiv(zDomain.upperBound - 1, spacingChunks); cellZ++) {
                    ChunkPos chunk = spread.getPotentialStructureChunk(seed, cellX * spacingChunks, cellZ * spacingChunks);
                    if (this.fold.isOver(chunk) || !spread.isStructureChunk(this.state, chunk.x(), chunk.z())) {
                        continue;
                    }

                    found.add(new StructurePick(chunk, selected(seed, chunk)));
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

        private Optional<StructureSet.StructureSelectionEntry> getEntry(Holder<Structure> structure) {
            return this.set.value().structures().stream()
                    .filter(entry -> entry.structure().equals(structure))
                    .findFirst();
        }

        private List<AddedStart> accepted(List<AddedStart> proposed) {
            if (proposed.isEmpty()) {
                return List.of();
            }

            Set<ChunkPos> picked = new HashSet<>();
            for (StructurePick pick : getPicks()) {
                picked.add(pick.chunk());
            }

            Set<AddedStart> seen = new HashSet<>();
            List<AddedStart> accepted = new ArrayList<>();
            for (AddedStart added : proposed) {
                String refusal = refusalOf(added, picked);
                if (refusal != null) {
                    ExoticWorlds.LOGGER.warn("[ew/structure-starts] dropped structure={} chunk={} reason={}",
                            added.structure().getRegisteredName(), added.chunk(), refusal);
                } else if (seen.add(added)) {
                    accepted.add(added);
                }
            }

            return List.copyOf(accepted);
        }

        private @Nullable String refusalOf(AddedStart added, Set<ChunkPos> picked) {
            if (this.fold.isOver(added.chunk())) {
                return "outside_bounds";
            }

            if (picked.contains(added.chunk())) {
                return "picked_chunk";
            }

            return getEntry(added.structure()).isPresent() ? null : "not_in_set";
        }
    }
}
