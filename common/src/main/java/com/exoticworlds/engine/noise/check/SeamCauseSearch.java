package com.exoticworlds.engine.noise.check;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.core.CuboidEdge;
import com.exoticworlds.core.DensityFunctionNodes;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.mixin.BiomeSourceAccessor;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.GradientNoise;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

final class SeamCauseSearch {
    private static final String VANILLA_PACKAGE = "net.minecraft.";

    private static final List<String> NOISE_CLASSES = Stream.of(NoiseStack.class, PerlinNoise.class,
            NormalNoise.class, SimplexNoise.class, BlendedNoise.class, GradientNoise.class)
            .map(Class::getName)
            .toList();

    record Point(int x, int y, int z) {
    }

    record Lap(Direction.Axis axis, int shiftX, int shiftZ) implements SeamPassage {
        static final int[] SAMPLE_X = {0, 100, 255};
        static final int[] SAMPLE_Z = {0, 137, -211};

        private static final int SAMPLE_Y = 64;

        private static final List<Point> SAMPLES = samplesOf();

        private static List<Point> samplesOf() {
            List<Point> samples = new ArrayList<>();
            for (int z : SAMPLE_Z) {
                for (int x : SAMPLE_X) {
                    samples.add(new Point(x, SAMPLE_Y, z));
                }
            }

            return List.copyOf(samples);
        }

        static List<Lap> of(TranslationLattice lattice) {
            List<Lap> laps = new ArrayList<>();
            if (lattice.x().loops()) {
                laps.add(new Lap(Direction.Axis.X, lattice.x().domainLength, 0));
            }

            if (lattice.z().loops()) {
                laps.add(new Lap(Direction.Axis.Z, lattice.skew(), lattice.z().domainLength));
            }

            return laps;
        }

        @Override
        public List<Point> samples() {
            return SAMPLES;
        }

        @Override
        public Point away(Point point) {
            return new Point(point.x() + this.shiftX, point.y(), point.z() + this.shiftZ);
        }

        @Override
        public <T> T reading(Point at, Supplier<T> read) {
            return read.get();
        }

        @Override
        public @Nullable CuboidEdge edge() {
            return null;
        }

        int widthBlocks() {
            return this.axis == Direction.Axis.X ? this.shiftX : this.shiftZ;
        }
    }

    private record Found(DensityFunction node, @Nullable Identifier holder) {
    }

    static List<SeamVerdict.Break> causesOf(Map<SeamField, List<Point>> broken, SeamPassage passage,
            RandomState state, BiomeSource biomes) {
        SeamField brokenClimate = broken.keySet().stream().filter(SeamField::climate).findFirst().orElse(null);
        List<SeamVerdict.Break> breaks = new ArrayList<>();
        broken.forEach((field, points) -> {
            DensityFunction root = field.densityOf(state.router);
            SeamCause cause;
            if (root != null) {
                cause = densityCause(root, points, passage, state);
            } else if (brokenClimate != null) {
                cause = new SeamCause.Follows(brokenClimate.id());
            } else {
                cause = biomeSourceCause(biomes);
            }

            breaks.add(new SeamVerdict.Break(field.id(), cause, passage.edge()));
        });
        return breaks;
    }

    static SeamCause densityCause(DensityFunction root, List<Point> points, SeamPassage passage,
            RandomState state) {
        return new Search(state, passage, points).causeUnder(root);
    }

    static SeamCause.Generator generatorCause(ChunkGenerator generator) {
        Identifier type = generator.getTypeNameForDataFixer().orElse(null);
        return type != null
                ? new SeamCause.Generator(type.toString(), type.getNamespace())
                : new SeamCause.Generator(generator.getClass().getName(), null);
    }

    private static SeamCause biomeSourceCause(BiomeSource biomes) {
        Identifier type = BuiltInRegistries.BIOME_SOURCE.getKey(((BiomeSourceAccessor) biomes).exotic_worlds$codec());
        return type != null
                ? new SeamCause.Biomes(type.toString(), type.getNamespace())
                : new SeamCause.Biomes(biomes.getClass().getName(), null);
    }

    private static final class Search {
        private final RandomState state;
        private final SeamPassage passage;
        private final List<Point> points;
        private final IdentityHashMap<DensityFunction, Boolean> breaks = new IdentityHashMap<>();

        Search(RandomState state, SeamPassage passage, List<Point> points) {
            this.state = state;
            this.passage = passage;
            this.points = points;
        }

        SeamCause causeUnder(DensityFunction root) {
            Found found = this.deepest(root, null);
            return found != null ? causeOf(found) : new SeamCause.NotFound();
        }

        private @Nullable Found deepest(DensityFunction node, @Nullable Identifier holder) {
            if (!this.breaks.computeIfAbsent(node, this::breaksAcross)) {
                return null;
            }

            Identifier enclosing = holderKeyOf(node, holder);
            for (DensityFunction child : DensityFunctionNodes.children(node)) {
                Found found = this.deepest(child, enclosing);
                if (found != null) {
                    return found;
                }
            }

            return new Found(node, holder);
        }

        private boolean breaksAcross(DensityFunction node) {
            DensitySampler sampler = this.state.getSampler(node);
            for (Point point : this.points) {
                if (this.passage.differsAt(sampler, point)) {
                    return true;
                }
            }

            return false;
        }
    }

    private static @Nullable Identifier holderKeyOf(DensityFunction node, @Nullable Identifier enclosing) {
        if (node instanceof DensityFunctions.HolderHolder(Holder<DensityFunction> holder)) {
            return holder.unwrapKey().map(ResourceKey::identifier).orElse(enclosing);
        }

        return enclosing;
    }

    private static SeamCause causeOf(Found found) {
        DensityFunction node = found.node();
        Identifier holderKey = found.holder();
        String holder = holderKey != null ? holderKey.toString() : null;
        Identifier type = typeKeyOf(node);
        String className = node.getClass().getName();
        boolean vanilla = type != null
                ? type.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)
                : className.startsWith(VANILLA_PACKAGE);
        List<String> candidates = vanilla
                ? AppliedMixins.foreignOn(Stream.concat(Stream.of(className), NOISE_CLASSES.stream()).toList())
                : List.of();
        return new SeamCause.Node(type != null ? type.toString() : className, holder,
                type != null ? type.getNamespace() : null, vanilla, candidates);
    }

    private static @Nullable Identifier typeKeyOf(DensityFunction node) {
        try {
            return BuiltInRegistries.DENSITY_FUNCTION_TYPE.getKey(node.codec());
        } catch (UnsupportedOperationException noCodec) {
            ExoticWorlds.LOGGER.debug("[ew/edge] node_without_type class={}", node.getClass().getName());
            return null;
        }
    }

    private SeamCauseSearch() {
    }
}
