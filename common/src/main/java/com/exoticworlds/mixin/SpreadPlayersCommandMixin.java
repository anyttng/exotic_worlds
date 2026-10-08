package com.exoticworlds.mixin;

import java.util.Locale;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;
import com.exoticworlds.engine.fold.SeamDelta;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic4CommandExceptionType;

import net.minecraft.server.commands.SpreadPlayersCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

@Mixin(SpreadPlayersCommand.class)
public class SpreadPlayersCommandMixin {
    @Unique
    private static final int MAX_ITERATION_COUNT = 10000;

    @Shadow
    @Final
    private static Dynamic4CommandExceptionType ERROR_FAILED_TO_SPREAD_TEAMS;

    @Shadow
    @Final
    private static Dynamic4CommandExceptionType ERROR_FAILED_TO_SPREAD_ENTITIES;

    @WrapMethod(method = "spreadPositions")
    private static void toroidal$spreadPositionsAcrossSeam(Vec2 center, double spreadDist, ServerLevel level,
            RandomSource random, double minX, double minZ, double maxX, double maxZ, int maxHeight,
            SpreadPlayersCommand.Position[] positions, boolean respectTeams, Operation<Void> original)
            throws CommandSyntaxException {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null || toroidal$fitsInHalfTheWorld(transformer, maxX - minX, maxZ - minZ)) {
            original.call(center, spreadDist, level, random, minX, minZ, maxX, maxZ, maxHeight, positions,
                    respectTeams);
            return;
        }

        // Vanilla's clamp, except where the square is the whole world: there the two edges are the same ground.
        AxisBounds xAxis = transformer.bounds().x();
        AxisBounds zAxis = transformer.bounds().z();
        boolean freeX = xAxis.coversWorld(maxX - minX);
        boolean freeZ = zAxis.coversWorld(maxZ - minZ);
        double randomMinX = freeX && xAxis instanceof AxisBounds.Looped looped ? looped.minBlock() : minX;
        double randomMaxX = freeX && xAxis instanceof AxisBounds.Looped looped ? looped.maxBlock() : maxX;
        double randomMinZ = freeZ && zAxis instanceof AxisBounds.Looped looped ? looped.minBlock() : minZ;
        double randomMaxZ = freeZ && zAxis instanceof AxisBounds.Looped looped ? looped.maxBlock() : maxZ;

        boolean hasCollisions = true;
        double minDistance = Float.MAX_VALUE;

        int iteration;
        for (iteration = 0; iteration < MAX_ITERATION_COUNT && hasCollisions; iteration++) {
            hasCollisions = false;
            minDistance = Float.MAX_VALUE;

            for (int i = 0; i < positions.length; i++) {
                SpreadPositionAccessor position = (SpreadPositionAccessor) positions[i];
                int neighbourCount = 0;

                double towardNeighboursX = 0.0;
                double towardNeighboursZ = 0.0;

                for (int j = 0; j < positions.length; j++) {
                    if (i == j) {
                        continue;
                    }

                    SpreadPositionAccessor neighbour = (SpreadPositionAccessor) positions[j];
                    Vec3 delta = toroidal$deltaBetween(transformer, position, neighbour);
                    double deltaX = delta.x;
                    double deltaZ = delta.z;
                    double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
                    minDistance = Math.min(distance, minDistance);
                    if (distance < spreadDist) {
                        neighbourCount++;
                        towardNeighboursX += deltaX;
                        towardNeighboursZ += deltaZ;
                    }
                }

                if (neighbourCount > 0) {
                    towardNeighboursX /= neighbourCount;
                    towardNeighboursZ /= neighbourCount;
                    double length = Math.sqrt(
                            towardNeighboursX * towardNeighboursX + towardNeighboursZ * towardNeighboursZ);
                    if (length > 0.0) {
                        position.toroidal$setX(position.toroidal$x() - towardNeighboursX / length);
                        position.toroidal$setZ(position.toroidal$z() - towardNeighboursZ / length);
                    } else {
                        positions[i].randomize(random, randomMinX, randomMinZ, randomMaxX, randomMaxZ);
                    }

                    hasCollisions = true;
                }

                if (toroidal$confine(position, transformer, freeX, freeZ, minX, minZ, maxX, maxZ)) {
                    hasCollisions = true;
                }
            }

            if (!hasCollisions) {
                for (SpreadPlayersCommand.Position position : positions) {
                    if (!position.isSafe(level, maxHeight)) {
                        position.randomize(random, randomMinX, randomMinZ, randomMaxX, randomMaxZ);
                        hasCollisions = true;
                    }
                }
            }
        }

        if (minDistance == Float.MAX_VALUE) {
            minDistance = 0.0;
        }

        if (iteration >= MAX_ITERATION_COUNT) {
            Dynamic4CommandExceptionType failure =
                    respectTeams ? ERROR_FAILED_TO_SPREAD_TEAMS : ERROR_FAILED_TO_SPREAD_ENTITIES;
            throw failure.create(
                    positions.length, center.x, center.y, String.format(Locale.ROOT, "%.2f", minDistance));
        }
    }

    @WrapOperation(
            method = "setPlayerPositions",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/commands/SpreadPlayersCommand$Position;dist(Lnet/minecraft/server/commands/SpreadPlayersCommand$Position;)D"))
    private static double toroidal$distThroughSeam(SpreadPlayersCommand.Position position,
            SpreadPlayersCommand.Position target, Operation<Double> original,
            @Local(argsOnly = true) ServerLevel level) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return original.call(position, target);
        }

        SpreadPositionAccessor from = (SpreadPositionAccessor) position;
        SpreadPositionAccessor to = (SpreadPositionAccessor) target;
        Vec3 delta = toroidal$deltaBetween(transformer, from, to);
        return Math.sqrt(delta.x * delta.x + delta.z * delta.z);
    }

    @Unique
    private static Vec3 toroidal$deltaBetween(WorldFold transformer, SpreadPositionAccessor from,
            SpreadPositionAccessor to) {
        return transformer.foldDelta(
                new Vec3(from.toroidal$x(), 0.0, from.toroidal$z()), new Vec3(to.toroidal$x(), 0.0, to.toroidal$z()));
    }

    @Unique
    private static boolean toroidal$fitsInHalfTheWorld(WorldFold transformer, double xSpan, double zSpan) {
        Vec3 diagonal = new Vec3(xSpan, 0.0, zSpan);
        Vec3 antiDiagonal = new Vec3(xSpan, 0.0, -zSpan);
        return SeamDelta.fold(transformer, diagonal).equals(diagonal)
                && SeamDelta.fold(transformer, antiDiagonal).equals(antiDiagonal);
    }

    @Unique
    private static boolean toroidal$confine(SpreadPositionAccessor position, WorldFold transformer,
            boolean freeX, boolean freeZ, double minX, double minZ, double maxX, double maxZ) {
        TranslationLattice lattice = transformer.blockLattice();
        boolean clamped = false;
        double x = position.toroidal$x();
        double z = position.toroidal$z();
        if (freeZ) {
            x -= (double) lattice.zLaps(z) * lattice.skew();
            z = lattice.foldZ(z);
        } else if (z < minZ) {
            z = minZ;
            clamped = true;
        } else if (z > maxZ) {
            z = maxZ;
            clamped = true;
        }

        if (freeX) {
            x = lattice.x().wrap(x);
        } else if (x < minX) {
            x = minX;
            clamped = true;
        } else if (x > maxX) {
            x = maxX;
            clamped = true;
        }

        position.toroidal$setX(x);
        position.toroidal$setZ(z);
        return clamped;
    }
}
