package com.exoticworlds.engine.gen;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.google.common.math.IntMath;
import com.google.common.math.LongMath;
import com.exoticworlds.accessors.ShapeStamp;
import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.world.CoordinateConstants;
import com.exoticworlds.core.world.DimensionTable;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeDerivations;
import com.exoticworlds.core.gen.GeneratorShapes;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.core.world.WorldLoopBounds.AxisBounds;
import com.exoticworlds.core.world.WorldLoopSizes;
import com.exoticworlds.engine.gen.DatapackStemOverrides.StemOverride;
import com.exoticworlds.platform.Platforms;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;

public final class WorldShapeReport {
    private static final String STAMP_SOURCE = "stamp";
    private static final String RESTORED_SOURCE = "restored";
    private static final String STORED_SOURCE = "stored";
    private static final String TABLE_SOURCE = "table";
    private static final String NOT_PERIODIC_NOTE = ", stamped, terrain not periodic";

    public record Line(boolean isBroken, String text) {
    }

    record Note(boolean isBroken, String text) {
        static final Note NONE = new Note(false, "");
    }

    public static List<Line> lines(MinecraftServer server) {
        FlatShape overworldShape = getShape(server.overworld());
        List<Line> lines = new ArrayList<>();
        boolean isAnyWrapped = false;
        for (ServerLevel level : server.getAllLevels()) {
            FlatShape shape = getShape(level);
            if (shape != null) {
                isAnyWrapped = true;
                lines.add(wrappedLine(server, level, shape));
            } else {
                lines.add(new Line(false, unwrappedLine(server, level, overworldShape)));
            }
        }

        return isAnyWrapped ? lines : List.of();
    }

    private static Line wrappedLine(MinecraftServer server, ServerLevel level, FlatShape shape) {
        WorldLoopBounds bounds = shape.bounds();
        StemOverride override = getOverride(level);
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        Note netherScale = netherScale(server, level, shape);
        Note endWidth = endWidth(level, bounds);
        return new Line(netherScale.isBroken() || endWidth.isBroken(), "World shape: " + level.dimension().identifier()
                + " generator=" + generatorId(generator)
                + " shape=" + shapeSource(server, level, override)
                + " identification=" + shape.getIdentification()
                + " x=" + bounds.x().getSpanText() + " z=" + bounds.z().getSpanText() + " chunks"
                + ", " + widths(bounds)
                + skew(shape)
                + seamTerrain(generator)
                + netherScale.text()
                + endWidth.text()
                + datapackOverride(override)
                + tail());
    }

    private static String unwrappedLine(MinecraftServer server, ServerLevel level, @Nullable FlatShape overworldShape) {
        return "World shape: " + level.dimension().identifier() + " not wrapped"
                + " generator=" + generatorId(level.getChunkSource().getGenerator())
                + ", " + unwrappedReason(server, level, overworldShape)
                + tail();
    }

    private static String unwrappedReason(MinecraftServer server, ServerLevel level, @Nullable FlatShape overworldShape) {
        if (getTableEntry(server, level) != null) {
            return "the overworld's dimension table leaves it unwrapped";
        }

        return overworldShape == null ? "neither it nor the overworld carries a shape"
                : underivedReason(server, level, overworldShape);
    }

    private static DimensionTable.@Nullable Entry getTableEntry(MinecraftServer server, ServerLevel level) {
        return ShapedDimensions.getDimensionTable(server.overworld().getChunkSource().getGenerator())
                .getEntry(Registries.levelToLevelStem(level.dimension()));
    }

    private static String underivedReason(MinecraftServer server, ServerLevel level, FlatShape overworldShape) {
        double overworldScale = server.overworld().dimensionType().coordinateScale();
        double scale = level.dimensionType().coordinateScale();
        CarriedShape overworld = GeneratorShapes.getCarriedShape(server.overworld());
        FlatShape derived = ShapeDerivations.derive(overworld == null ? null : overworld.getShapeId().orElse(null),
                overworldShape, Registries.levelToLevelStem(level.dimension()), overworldScale, scale);
        return derived == null
                ? "coordinate scale " + scale + " derives no whole-chunk width in range or whole-chunk skew from the"
                        + " overworld's " + widths(overworldShape.bounds()) + skew(overworldShape)
                : "a derivable " + widths(derived.bounds()) + " never reached the generator";
    }

    private static String seamTerrain(ChunkGenerator generator) {
        return GeneratorShapes.isTilingAtSeam(generator) ? "" : NOT_PERIODIC_NOTE;
    }

    private static String shapeSource(MinecraftServer server, ServerLevel level, @Nullable StemOverride override) {
        if (override != null) {
            return switch (override.outcome()) {
                case RESHAPED -> RESTORED_SOURCE;
                case STAMPED -> STAMP_SOURCE;
                case REFUSED -> STORED_SOURCE;
            };
        }

        ChunkGenerator generator = level.getChunkSource().getGenerator();
        boolean hasOwnStamp = generator instanceof ShapeStamp stamp && stamp.exotic_worlds$getCarriedShape() != null;
        return !hasOwnStamp && getTableEntry(server, level) != null ? TABLE_SOURCE : STAMP_SOURCE;
    }

    private static @Nullable StemOverride getOverride(ServerLevel level) {
        return DatapackStemOverrides.of(Registries.levelToLevelStem(level.dimension()));
    }

    private static String datapackOverride(@Nullable StemOverride override) {
        if (override == null) {
            return "";
        }

        return ", a datapack " + override.datapackGenerator() + " " + switch (override.outcome()) {
            case RESHAPED -> "took the stored shape";
            case STAMPED -> "was stamped with the stored shape";
            case REFUSED -> "was refused the stored shape";
        };
    }

    private static String tail() {
        return " | mod=" + Platforms.get().modVersion()
                + " mc=" + SharedConstants.getCurrentVersion().name()
                + " loader=" + Platforms.get().loaderName() + " " + Platforms.get().loaderVersion();
    }

    private static @Nullable FlatShape getShape(@Nullable ServerLevel level) {
        CarriedShape carried = level == null ? null : GeneratorShapes.getCarriedShape(level);
        return carried == null ? null : carried.getShape();
    }

    private static String generatorId(ChunkGenerator generator) {
        return generator.getClass().getSimpleName();
    }

    private static String widths(WorldLoopBounds bounds) {
        if (bounds.isSquare()) {
            return "width " + WorldLoopSizes.describe(bounds.getWidthChunks(Direction.Axis.X));
        }

        StringBuilder widths = new StringBuilder("width");
        if (bounds.x() instanceof AxisBounds.Looped xLooped) {
            widths.append(" x=").append(WorldLoopSizes.describe(xLooped.getWidthChunks()));
        }

        if (bounds.z() instanceof AxisBounds.Looped zLooped) {
            widths.append(" z=").append(WorldLoopSizes.describe(zLooped.getWidthChunks()));
        }

        return widths.toString();
    }

    private static String skew(FlatShape shape) {
        return shape.skewChunks() == FlatShape.NO_SKEW ? "" : ", skew " + shape.skewChunks() + " chunks";
    }

    private static Note netherScale(MinecraftServer server, ServerLevel level, FlatShape shape) {
        if (level.dimension() != Level.NETHER) {
            return Note.NONE;
        }

        FlatShape overworldShape = getShape(server.overworld());
        if (overworldShape == null) {
            return Note.NONE;
        }

        Note skew = netherSkewNote(overworldShape, shape);
        return skew.isBroken() ? skew : new Note(false, netherScaleText(overworldShape.bounds(), shape.bounds()));
    }

    static Note netherSkewNote(FlatShape overworld, FlatShape nether) {
        if (!hasSharedLoop(overworld.bounds(), nether.bounds(), Direction.Axis.X)) {
            return Note.NONE;
        }

        long overworldWidthChunks = overworld.bounds().getWidthChunks(Direction.Axis.X);
        long netherWidthChunks = nether.bounds().getWidthChunks(Direction.Axis.X);
        long overworldSkewChunks = overworld.skewChunks();
        long netherSkewChunks = nether.skewChunks();
        if (Math.floorMod(overworldSkewChunks * netherWidthChunks - netherSkewChunks * overworldWidthChunks, overworldWidthChunks * netherWidthChunks)
                == 0) {
            return Note.NONE;
        }

        return new Note(true, ", BROKEN nether skew: an overworld of " + overworldWidthChunks + " chunks skewed by "
                + overworldSkewChunks + " chunks needs a nether of " + netherWidthChunks + " chunks skewed by "
                + fraction(overworldSkewChunks * netherWidthChunks, overworldWidthChunks) + " chunks, not " + netherSkewChunks
                + ", so portals will not line up across the Z seam");
    }

    static String netherScaleText(WorldLoopBounds overworld, WorldLoopBounds nether) {
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (hasSharedLoop(overworld, nether, axis)) {
                int netherWidthChunks = nether.getWidthChunks(axis);
                int overworldWidthChunks = overworld.getWidthChunks(axis);
                int common = IntMath.gcd(netherWidthChunks, overworldWidthChunks);
                return ", scale " + netherWidthChunks / common + ":" + overworldWidthChunks / common;
            }
        }

        return "";
    }

    private static String fraction(long numerator, long denominator) {
        long common = LongMath.gcd(Math.abs(numerator), denominator);
        return denominator == common ? Long.toString(numerator / common)
                : numerator / common + "/" + denominator / common;
    }

    private static Note endWidth(ServerLevel level, WorldLoopBounds bounds) {
        return level.dimension() != Level.END ? Note.NONE : endWidthNote(bounds);
    }

    static Note endWidthNote(WorldLoopBounds end) {
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (end.isLooped(axis) && end.getWidthChunks(axis) < WorldLoopSizes.END_MIN_WIDTH_CHUNKS) {
                return new Note(true, ", BROKEN End width on the " + axis.getName() + " axis: "
                        + WorldLoopSizes.describe(end.getWidthChunks(axis)) + " is under the "
                        + WorldLoopSizes.describe(WorldLoopSizes.END_MIN_WIDTH_CHUNKS)
                        + " the outer islands need, so no end cities and no elytra");
            }
        }

        return Note.NONE;
    }

    private static boolean hasSharedLoop(WorldLoopBounds overworld, WorldLoopBounds nether, Direction.Axis axis) {
        return overworld.isLooped(axis) && nether.isLooped(axis);
    }

    private WorldShapeReport() {
    }
}
