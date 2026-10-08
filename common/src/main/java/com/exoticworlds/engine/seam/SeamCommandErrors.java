package com.exoticworlds.engine.seam;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;
import com.exoticworlds.engine.fold.SeamSpans;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class SeamCommandErrors {
    // Escaped as vanilla escapes its own dynamic errors: a non-Component argument makes TranslatableContents throw.
    private static final Dynamic3CommandExceptionType COORDINATE_OUTSIDE_WORLD = new Dynamic3CommandExceptionType(
            (coord, min, max) -> Component.translatableEscape(
                    "argument.exotic_worlds.pos.outside_world", coord, min, max));

    private static final SimpleCommandExceptionType REGION_ACROSS_SEAM = new SimpleCommandExceptionType(
            Component.translatable("commands.exotic_worlds.region.across_seam"));

    public static void requireInsideWorld(WorldFold fold, Direction.Axis axis, WorldCoordinate coordinate)
            throws CommandSyntaxException {
        if (coordinate.isRelative()) {
            return;
        }

        // get() adds the origin only for a relative coordinate, and the branch above has already returned on those, so
        // it hands back the typed value itself. 1.21.1 keeps the field private and offers no other way to read it.
        requireInsideWorld(fold, axis, coordinate.get(0.0));
    }

    public static void requireInsideWorld(WorldFold fold, Direction.Axis axis, double coord)
            throws CommandSyntaxException {
        if (!(fold.bounds().axis(axis) instanceof AxisBounds.Looped looped) || !looped.isOver(coord)
                || isForeign(fold.blockLattice(), axis, coord)) {
            return;
        }

        throw COORDINATE_OUTSIDE_WORLD.create(blockOf(coord), looped.minBlock(), looped.maxBlock() - 1);
    }

    private static boolean isForeign(TranslationLattice lattice, Direction.Axis axis, double coord) {
        return (axis == Direction.Axis.X ? lattice.x() : lattice.z()).isForeign(coord);
    }

    private static long blockOf(double coord) {
        return (long) Math.floor(coord);
    }

    public static @Nullable CommandSyntaxException refusalForAmbiguousRegion(
            @Nullable WorldFold transformer, BoundingBox region) {
        if (transformer == null || !SeamSpans.crossesSeam(transformer, region)) {
            return null;
        }

        return REGION_ACROSS_SEAM.create();
    }

    public static void requireUnambiguousRegion(@Nullable WorldFold transformer, BoundingBox region)
            throws CommandSyntaxException {
        CommandSyntaxException refusal = refusalForAmbiguousRegion(transformer, region);
        if (refusal != null) {
            throw refusal;
        }
    }

    private SeamCommandErrors() {
    }
}
