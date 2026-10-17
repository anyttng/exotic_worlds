package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;
import com.exoticworlds.engine.fold.SeamDelta;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import net.minecraft.server.commands.SpreadPlayersCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

@Mixin(SpreadPlayersCommand.class)
public class SpreadPlayersCommandMixin {
    @Unique
    private static final String SPREAD_FOLD = "spreadFold";

    @Inject(method = "spreadPositions", at = @At("HEAD"))
    private static void toroidal$resolveSpreadFold(Vec2 center, double spreadDist, ServerLevel level,
            RandomSource random, double minX, double minZ, double maxX, double maxZ, int maxHeight,
            SpreadPlayersCommand.Position[] positions, boolean respectTeams, CallbackInfo ci,
            @Share(SPREAD_FOLD) LocalRef<@Nullable WorldFold> spreadFold) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer != null && !toroidal$fitsInHalfTheWorld(transformer, maxX - minX, maxZ - minZ)) {
            spreadFold.set(transformer);
        }
    }

    @ModifyVariable(method = "spreadPositions", at = @At("STORE"), name = "neighbour")
    private static SpreadPlayersCommand.Position toroidal$neighborThroughSeam(
            SpreadPlayersCommand.Position neighbor, @Local(name = "position") SpreadPlayersCommand.Position position,
            @Share(SPREAD_FOLD) LocalRef<@Nullable WorldFold> spreadFold) {
        WorldFold transformer = spreadFold.get();
        if (transformer == null) {
            return neighbor;
        }

        SpreadPositionAccessor from = (SpreadPositionAccessor) position;
        SpreadPositionAccessor to = (SpreadPositionAccessor) neighbor;
        Vec3 raw = new Vec3(to.toroidal$x(), 0.0, to.toroidal$z());
        Vec3 nearest = transformer.nearestCopy(new Vec3(from.toroidal$x(), 0.0, from.toroidal$z()), raw);
        if (nearest == raw) {
            return neighbor;
        }

        SpreadPlayersCommand.Position copy = SpreadPositionAccessor.toroidal$create();
        ((SpreadPositionAccessor) copy).toroidal$setX(nearest.x);
        ((SpreadPositionAccessor) copy).toroidal$setZ(nearest.z);
        return copy;
    }

    @WrapOperation(
            method = "spreadPositions",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/commands/SpreadPlayersCommand$Position;clamp(DDDD)Z"))
    private static boolean toroidal$confineAcrossSeam(SpreadPlayersCommand.Position position, double minX,
            double minZ, double maxX, double maxZ, Operation<Boolean> original,
            @Share(SPREAD_FOLD) LocalRef<@Nullable WorldFold> spreadFold) {
        WorldFold transformer = spreadFold.get();
        if (transformer == null) {
            return original.call(position, minX, minZ, maxX, maxZ);
        }

        return toroidal$confine((SpreadPositionAccessor) position, transformer, minX, minZ, maxX, maxZ);
    }

    @WrapOperation(
            method = "spreadPositions",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/commands/SpreadPlayersCommand$Position;"
                            + "randomize(Lnet/minecraft/util/RandomSource;DDDD)V"))
    private static void toroidal$randomizeAcrossSeam(SpreadPlayersCommand.Position position, RandomSource random,
            double minX, double minZ, double maxX, double maxZ, Operation<Void> original,
            @Share(SPREAD_FOLD) LocalRef<@Nullable WorldFold> spreadFold) {
        WorldFold transformer = spreadFold.get();
        if (transformer == null) {
            original.call(position, random, minX, minZ, maxX, maxZ);
            return;
        }

        AxisBounds xAxis = transformer.bounds().x();
        AxisBounds zAxis = transformer.bounds().z();
        original.call(position, random,
                toroidal$randomMin(xAxis, minX, maxX), toroidal$randomMin(zAxis, minZ, maxZ),
                toroidal$randomMax(xAxis, minX, maxX), toroidal$randomMax(zAxis, minZ, maxZ));
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
        Vec3 delta = transformer.foldDelta(
                new Vec3(from.toroidal$x(), 0.0, from.toroidal$z()), new Vec3(to.toroidal$x(), 0.0, to.toroidal$z()));
        return Math.sqrt(delta.x * delta.x + delta.z * delta.z);
    }

    @Unique
    private static boolean toroidal$fitsInHalfTheWorld(WorldFold transformer, double xSpan, double zSpan) {
        Vec3 diagonal = new Vec3(xSpan, 0.0, zSpan);
        Vec3 antiDiagonal = new Vec3(xSpan, 0.0, -zSpan);
        return SeamDelta.fold(transformer, diagonal).equals(diagonal)
                && SeamDelta.fold(transformer, antiDiagonal).equals(antiDiagonal);
    }

    @Unique
    private static double toroidal$randomMin(AxisBounds axis, double min, double max) {
        return axis.coversWorld(max - min) && axis instanceof AxisBounds.Looped looped ? looped.minBlock() : min;
    }

    @Unique
    private static double toroidal$randomMax(AxisBounds axis, double min, double max) {
        return axis.coversWorld(max - min) && axis instanceof AxisBounds.Looped looped ? looped.maxBlock() : max;
    }

    @Unique
    private static boolean toroidal$confine(SpreadPositionAccessor position, WorldFold transformer,
            double minX, double minZ, double maxX, double maxZ) {
        boolean freeX = transformer.bounds().x().coversWorld(maxX - minX);
        boolean freeZ = transformer.bounds().z().coversWorld(maxZ - minZ);
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
