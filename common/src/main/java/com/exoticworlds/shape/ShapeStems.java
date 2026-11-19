package com.exoticworlds.shape;

import java.util.List;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.api.v1.shape.ShapeDimensions;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.core.NetherScales;
import com.exoticworlds.engine.gen.ShapedDimensions;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class ShapeStems {

    public static @Nullable LoopSpans spansOf(WorldDimensions dimensions, ResourceKey<LevelStem> key,
            Predicate<LoopSpans> ownGeometry) {
        LoopSpans spans = ShapeDimensions.spansOf(dimensions, key);
        return spans != null && ownGeometry.test(spans) ? spans : null;
    }

    public static boolean carriesDimensionTable(WorldDimensions dimensions) {
        return !ShapedDimensions.dimensionTableOf(dimensions).isEmpty();
    }

    public static boolean isOwnOrAbsent(WorldDimensions dimensions, ResourceKey<LevelStem> key,
            Predicate<LoopSpans> ownGeometry) {
        if (dimensions.get(key).isEmpty()) {
            return true;
        }

        LoopSpans spans = ShapeDimensions.spansOf(dimensions, key);
        return spans != null && ownGeometry.test(spans);
    }

    public static WorldDimensions withSpans(WorldDimensions dimensions, StemSettings settings) {
        WrittenStems written = WrittenStems.of(settings);
        return ShapedDimensions.withShapes(dimensions, written.overworld(), written.nether(), written.end());
    }

    public static boolean writesBack(WorldDimensions dimensions, StemSettings settings) {
        WrittenStems written = WrittenStems.of(settings);
        return isStoredOrAbsent(dimensions, LevelStem.OVERWORLD, written.overworld())
                && isStoredOrAbsent(dimensions, LevelStem.NETHER, written.nether())
                && isStoredOrAbsent(dimensions, LevelStem.END, written.end());
    }

    private static boolean isStoredOrAbsent(WorldDimensions dimensions, ResourceKey<LevelStem> key,
            CarriedShape written) {
        if (dimensions.get(key).isEmpty()) {
            return true;
        }

        CarriedShape stored = ShapedDimensions.carriedShapeOf(dimensions, key);
        return stored != null && written.equals(stored);
    }

    private record WrittenStems(CarriedShape overworld, CarriedShape nether, CarriedShape end) {
        private static WrittenStems of(StemSettings settings) {
            GenerationOptions options = settings.generationOptions();
            GenerationOptions withoutSpawn = options.with(SpawnPoint.OPTION, SpawnPoint.DEFAULT);
            return new WrittenStems(
                    new CarriedShape(FlatShape.of(settings.overworld()), options),
                    new CarriedShape(FlatShape.of(netherOf(settings.overworld(), settings.netherScale())), withoutSpawn),
                    new CarriedShape(FlatShape.of(settings.end()), withoutSpawn));
        }
    }

    public static int readNetherScale(WorldDimensions dimensions, Predicate<LoopSpans> ownGeometry,
            LoopSpans overworld, Direction.Axis axis) {
        LoopSpans nether = spansOf(dimensions, LevelStem.NETHER, ownGeometry);
        int stored = nether != null && nether.loops(axis)
                ? overworld.chunkWidth(axis) / nether.chunkWidth(axis)
                : NetherScales.DEFAULT;
        return normalizeNetherScale(stored, overworld);
    }

    public static LoopSpans netherOf(LoopSpans overworld, int netherScale) {
        return overworld.scaledDown(normalizeNetherScale(netherScale, overworld));
    }

    public static List<Integer> netherScalesFor(LoopSpans overworld) {
        int xChunkWidth = overworld.loops(Direction.Axis.X)
                ? overworld.chunkWidth(Direction.Axis.X)
                : overworld.chunkWidth(Direction.Axis.Z);
        int zChunkWidth = overworld.loops(Direction.Axis.Z) ? overworld.chunkWidth(Direction.Axis.Z) : xChunkWidth;
        return NetherScales.allowedFor(xChunkWidth, zChunkWidth, overworld.skewChunks());
    }

    private static int normalizeNetherScale(int netherScale, LoopSpans overworld) {
        return NetherScales.normalize(netherScale, netherScalesFor(overworld));
    }

    private ShapeStems() {
    }
}
