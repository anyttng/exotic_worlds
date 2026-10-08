package com.exoticworlds.engine.gen;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.ShapeStamp;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;
import com.exoticworlds.core.WorldLoopSizes;
import com.exoticworlds.engine.gen.DatapackStemOverrides.Outcome;
import com.exoticworlds.engine.gen.DatapackStemOverrides.StemOverride;
import com.exoticworlds.platform.Platforms;

import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class ShapedDimensions {

    public static WorldDimensions withShape(WorldDimensions dimensions, ResourceKey<LevelStem> key,
            CarriedShape carried) {
        LevelStem stem = dimensions.get(key).orElse(null);
        if (stem == null) {
            return dimensions;
        }

        ChunkGenerator rebuilt = shapedGeneratorFor(stem.generator(), carried);
        ChunkGenerator marked = rebuilt != null ? rebuilt : stampedGeneratorFor(stem.generator(), carried);
        if (marked == null) {
            return dimensions;
        }

        Map<ResourceKey<LevelStem>, LevelStem> stems = new HashMap<>(dimensions.dimensions());
        stems.put(key, Platforms.get().withGenerator(stem, marked));
        return new WorldDimensions(stems);
    }

    public static WorldDimensions withShapes(WorldDimensions dimensions, CarriedShape overworld, CarriedShape nether,
            CarriedShape end) {
        WorldDimensions withOverworld = withShape(dimensions, LevelStem.OVERWORLD, overworld);
        if (withOverworld == dimensions) {
            return dimensions;
        }

        return withShape(withShape(withOverworld, LevelStem.NETHER, nether), LevelStem.END, end);
    }

    public static WorldDimensions stripShapes(WorldDimensions dimensions) {
        Map<ResourceKey<LevelStem>, LevelStem> stripped = null;
        for (Map.Entry<ResourceKey<LevelStem>, LevelStem> entry : dimensions.dimensions().entrySet()) {
            LevelStem stem = entry.getValue();
            if (stem.generator() instanceof ShapeStamp stamp) {
                stamp.toroidal$clearStamp();
            }

            if (!(stem.generator() instanceof ShapedChunkGenerator shaped)) {
                continue;
            }

            if (stripped == null) {
                stripped = new HashMap<>(dimensions.dimensions());
            }

            stripped.put(entry.getKey(), Platforms.get().withGenerator(stem, shaped.unshaped()));
        }

        return stripped == null ? dimensions : new WorldDimensions(stripped);
    }

    public static Registry<LevelStem> restoreStoredShapes(WorldDimensions stored, Registry<LevelStem> datapackDimensions) {
        Map<ResourceKey<LevelStem>, LevelStem> restored = new HashMap<>();
        Map<ResourceKey<LevelStem>, StemOverride> overrides = new HashMap<>();
        for (Holder.Reference<LevelStem> entry : datapackDimensions.listElements().toList()) {
            LevelStem datapackStem = entry.value();
            LevelStem storedStem = stored.get(entry.key()).orElse(null);
            if (storedStem == null || datapackStem.generator() instanceof ShapedChunkGenerator) {
                continue;
            }

            CarriedShape carried = ShapedChunkGenerator.carriedShapeOf(storedStem.generator());
            if (carried == null) {
                continue;
            }

            ChunkGenerator shaped = withStoredShape(datapackStem.generator(), carried);
            restored.put(entry.key(),
                    shaped == null ? storedStem : Platforms.get().withGenerator(datapackStem, shaped));
            overrides.put(entry.key(), override(outcomeOf(shaped), datapackStem));
        }

        DatapackStemOverrides.replaceAll(overrides);
        return restored.isEmpty() ? datapackDimensions : withStems(datapackDimensions, restored);
    }

    public static @Nullable ChunkGenerator withStoredShape(ChunkGenerator replacement, CarriedShape carried) {
        ChunkGenerator rebuilt = shapedGeneratorFor(replacement, carried);
        if (rebuilt != null) {
            return rebuilt;
        }

        return isStampableOverStoredShape(replacement) ? stampedGeneratorFor(replacement, carried) : null;
    }

    private static Outcome outcomeOf(@Nullable ChunkGenerator shaped) {
        if (shaped == null) {
            return Outcome.REFUSED;
        }

        return shaped instanceof ShapedChunkGenerator ? Outcome.RESHAPED : Outcome.STAMPED;
    }

    private static StemOverride override(Outcome outcome, LevelStem datapackStem) {
        return new StemOverride(outcome, datapackStem.generator().getClass().getSimpleName());
    }

    private static Registry<LevelStem> withStems(Registry<LevelStem> dimensions,
            Map<ResourceKey<LevelStem>, LevelStem> replacements) {
        WritableRegistry<LevelStem> rebuilt = new MappedRegistry<>(Registries.LEVEL_STEM,
                dimensions.registryLifecycle());
        dimensions.listElements().forEach(entry -> rebuilt.register(
                entry.key(),
                replacements.getOrDefault(entry.key(), entry.value()),
                dimensions.registrationInfo(entry.key()).orElse(RegistrationInfo.BUILT_IN)));
        return rebuilt.freeze();
    }

    public static void stampDerived(Registry<LevelStem> dimensions) {
        LevelStem overworld = dimensions.getOptional(LevelStem.OVERWORLD).orElse(null);
        if (overworld == null) {
            return;
        }

        CarriedShape carried = ShapedChunkGenerator.carriedShapeOf(overworld.generator());
        if (carried == null) {
            return;
        }

        FlatShape worldShape = carried.shape();
        if (worldShape.mirror() != null) {
            return;
        }

        double overworldScale = overworld.type().value().coordinateScale();
        dimensions.listElements().forEach(entry -> {
            LevelStem stem = entry.value();
            if (entry.key() == LevelStem.OVERWORLD
                    || carriesShape(stem.generator())
                    || !(stem.generator() instanceof ShapeStamp stamp)) {
                return;
            }

            FlatShape derived = derivedShape(worldShape, overworldScale, stem.type().value().coordinateScale());
            if (derived != null) {
                stamp.toroidal$stamp(carried.withShape(derived));
            }
        });
    }

    public static WorldDimensions keepChosenShape(WorldDimensions created, WorldDimensions chosen) {
        return isShaped(chosen) && !isShaped(created) ? chosen : created;
    }

    private static boolean isShaped(WorldDimensions dimensions) {
        return dimensions.dimensions().values().stream()
                .anyMatch(stem -> ShapedChunkGenerator.carriedShapeOf(stem.generator()) != null);
    }

    public static @Nullable CarriedShape carriedShapeOf(WorldDimensions dimensions, ResourceKey<LevelStem> key) {
        LevelStem stem = dimensions.get(key).orElse(null);
        return stem == null ? null : ShapedChunkGenerator.carriedShapeOf(stem.generator());
    }

    public static @Nullable FlatShape shapeOf(WorldDimensions dimensions, ResourceKey<LevelStem> key) {
        CarriedShape carried = carriedShapeOf(dimensions, key);
        return carried == null ? null : carried.shape();
    }

    public static @Nullable FlatShape derivedShape(FlatShape worldShape, double overworldScale, double scale) {
        if (overworldScale <= 0.0 || scale <= 0.0) {
            return null;
        }

        AxisBounds x = derivedAxis(worldShape.bounds().x(), overworldScale, scale);
        AxisBounds z = derivedAxis(worldShape.bounds().z(), overworldScale, scale);
        Integer skewChunks = derivedChunks(worldShape.skewChunks(), overworldScale, scale);
        return x == null || z == null || skewChunks == null
                ? null
                : new FlatShape(new WorldLoopBounds(x, z), skewChunks, null);
    }

    private static @Nullable Integer derivedChunks(int chunks, double overworldScale, double scale) {
        double derived = chunks * overworldScale / scale;
        int whole = (int) derived;
        return derived == whole ? whole : null;
    }

    private static @Nullable AxisBounds derivedAxis(AxisBounds axis, double overworldScale, double scale) {
        if (!(axis instanceof AxisBounds.Looped looped)) {
            return axis;
        }

        Integer chunkWidth = derivedChunks(looped.chunkWidth(), overworldScale, scale);
        return chunkWidth != null && WorldLoopSizes.isInRange(chunkWidth)
                ? AxisBounds.Looped.ofWidth(chunkWidth)
                : null;
    }

    private static @Nullable ChunkGenerator shapedGeneratorFor(ChunkGenerator generator, CarriedShape carried) {
        ChunkGenerator base = baseOf(generator);
        if (!isRebuildable(base)) {
            return null;
        }

        return base instanceof NoiseBasedChunkGenerator noise
                ? new LoopedChunkGenerator(noise.getBiomeSource(), noise.generatorSettings(), carried)
                : new LoopedFlatChunkGenerator(((FlatLevelSource) base).settings(), carried);
    }

    private static @Nullable ChunkGenerator stampedGeneratorFor(ChunkGenerator generator, CarriedShape carried) {
        ChunkGenerator base = baseOf(generator);
        if (!(base instanceof ShapeStamp stamp)) {
            return null;
        }

        stamp.toroidal$stamp(carried);
        return base;
    }

    private static boolean carriesShape(ChunkGenerator generator) {
        return generator instanceof ShapedChunkGenerator
                || (generator instanceof ShapeStamp stamp && stamp.toroidal$carriedShape() != null);
    }

    private static ChunkGenerator baseOf(ChunkGenerator generator) {
        return generator instanceof ShapedChunkGenerator shaped ? shaped.unshaped() : generator;
    }

    private static boolean isRebuildable(ChunkGenerator base) {
        return base.getClass() == NoiseBasedChunkGenerator.class || base.getClass() == FlatLevelSource.class;
    }

    private static boolean isStampableOverStoredShape(ChunkGenerator datapackGenerator) {
        return datapackGenerator instanceof NoiseBasedChunkGenerator;
    }

    private ShapedDimensions() {
    }
}
