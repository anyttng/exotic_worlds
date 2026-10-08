package com.exoticworlds.engine.seam;

import org.jspecify.annotations.Nullable;

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

        requireInsideWorld(fold, axis, coordinate.value());
    }

    public static void requireInsideWorld(WorldFold fold, Direction.Axis axis, double coord)
            throws CommandSyntaxException {
        if (!(fold.bounds().axis(axis) instanceof AxisBounds.Looped looped) || !looped.isOver(coord)) {
            return;
        }

        throw COORDINATE_OUTSIDE_WORLD.create(blockOf(coord), looped.minBlock(), looped.maxBlock() - 1);
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
