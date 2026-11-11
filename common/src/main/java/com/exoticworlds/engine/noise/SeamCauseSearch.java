package com.exoticworlds.engine.noise;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.mixin.BiomeSourceAccessor;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

final class SeamCauseSearch {
    private static final String VANILLA_PACKAGE = "net.minecraft.";

    private static final List<String> NOISE_CLASSES = Stream.of(PerlinNoise.class,
            NormalNoise.class, SimplexNoise.class, BlendedNoise.class, ImprovedNoise.class)
            .map(Class::getName)
            .toList();

    record Point(int x, int y, int z) {
    }

    record Lap(Direction.Axis axis, int widthBlocks) {
        Point away(Point point) {
            return this.axis == Direction.Axis.X
                    ? new Point(point.x() + this.widthBlocks, point.y(), point.z())
                    : new Point(point.x(), point.y(), point.z() + this.widthBlocks);
        }
    }

    private record Found(DensityFunction node, @Nullable Identifier holder) {
    }

    static List<SeamVerdict.Break> causesOf(Map<SeamField, List<Point>> broken, Lap lap, RandomState state,
            WorldFold fold, BiomeSource biomes) {
        SeamField brokenClimate = broken.keySet().stream().filter(SeamField::climate).findFirst().orElse(null);
        List<SeamVerdict.Break> breaks = new ArrayList<>();
        broken.forEach((field, points) -> {
            DensityFunction root = field.densityOf(state.router());
            SeamCause cause;
            if (root != null) {
                cause = densityCause(root, points, lap, fold);
            } else if (brokenClimate != null) {
                cause = new SeamCause.Follows(brokenClimate.id());
            } else {
                cause = biomeSourceCause(biomes);
            }

            breaks.add(new SeamVerdict.Break(field.id(), cause));
        });
        return breaks;
    }

    static SeamCause densityCause(DensityFunction root, List<Point> points, Lap lap, WorldFold fold) {
        return new Search(fold, lap, points).causeUnder(root);
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
        private final WorldFold fold;
        private final Lap lap;
        private final List<Point> points;
        private final IdentityHashMap<DensityFunction, Boolean> breaks = new IdentityHashMap<>();

        Search(WorldFold fold, Lap lap, List<Point> points) {
            this.fold = fold;
            this.lap = lap;
            this.points = points;
        }

        SeamCause causeUnder(DensityFunction root) {
            Found found = this.deepest(root, null);
            return found != null ? causeOf(found) : new SeamCause.NotFound();
        }

        private @Nullable Found deepest(DensityFunction node, @Nullable Identifier holder) {
            if (!this.breaks.computeIfAbsent(node, this::breaksAtLap)) {
                return null;
            }

            Identifier enclosing = holderKeyOf(node, holder);
            for (DensityFunction child : children(node)) {
                Found found = this.deepest(child, enclosing);
                if (found != null) {
                    return found;
                }
            }

            return new Found(node, holder);
        }

        private boolean breaksAtLap(DensityFunction node) {
            for (Point point : this.points) {
                Point away = this.lap.away(point);
                double here = this.sample(node, point);
                double lapAway = this.sample(node, away);
                if (Double.doubleToLongBits(here) != Double.doubleToLongBits(lapAway)) {
                    return true;
                }
            }

            return false;
        }

        private double sample(DensityFunction node, Point point) {
            DensityFunction.FunctionContext at = new DensityFunction.SinglePointContext(point.x(), point.y(),
                    point.z());
            return GenerationFoldContext.withFold(this.fold, () -> node.compute(at));
        }
    }

    private static List<DensityFunction> children(DensityFunction node) {
        if (node instanceof DensityFunctions.HolderHolder(Holder<DensityFunction> holder) && !holder.isBound()) {
            return List.of();
        }

        List<DensityFunction> children = new ArrayList<>();
        node.mapChildren(child -> {
            children.add(child);
            return child;
        });
        return children;
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
            return BuiltInRegistries.DENSITY_FUNCTION_TYPE.getKey(node.codec().codec());
        } catch (UnsupportedOperationException noCodec) {
            ExoticWorlds.LOGGER.debug("[ew/edge] node_without_type class={}", node.getClass().getName());
            return null;
        }
    }

    private SeamCauseSearch() {
    }
}
