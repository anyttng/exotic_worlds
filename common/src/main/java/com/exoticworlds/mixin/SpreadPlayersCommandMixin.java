package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;
import com.exoticworlds.engine.seam.SeamRange;
import com.exoticworlds.engine.seam.SpreadArea;
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
    private static void exotic_worlds$resolveSpreadFold(Vec2 center, double spreadDist, ServerLevel level,
            RandomSource random, double minX, double minZ, double maxX, double maxZ, int maxHeight,
            SpreadPlayersCommand.Position[] positions, boolean respectTeams, CallbackInfo ci,
            @Share(SPREAD_FOLD) LocalRef<@Nullable WorldFold> spreadFold) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer != null && !SpreadArea.fitsInHalfTheWorld(transformer, maxX - minX, maxZ - minZ)) {
            spreadFold.set(transformer);
        }
    }

    @ModifyVariable(method = "spreadPositions", at = @At("STORE"), name = "neighbour")
    private static SpreadPlayersCommand.Position exotic_worlds$neighborThroughSeam(
            SpreadPlayersCommand.Position neighbor, @Local(name = "position") SpreadPlayersCommand.Position position,
            @Share(SPREAD_FOLD) LocalRef<@Nullable WorldFold> spreadFold) {
        WorldFold transformer = spreadFold.get();
        if (transformer == null) {
            return neighbor;
        }

        SpreadPositionAccessor from = (SpreadPositionAccessor) position;
        SpreadPositionAccessor to = (SpreadPositionAccessor) neighbor;
        Vec3 raw = new Vec3(to.exotic_worlds$x(), 0.0, to.exotic_worlds$z());
        Vec3 nearest = transformer.nearestCopy(new Vec3(from.exotic_worlds$x(), 0.0, from.exotic_worlds$z()), raw);
        if (nearest == raw) {
            return neighbor;
        }

        SpreadPlayersCommand.Position copy = SpreadPositionAccessor.exotic_worlds$create();
        ((SpreadPositionAccessor) copy).exotic_worlds$setX(nearest.x);
        ((SpreadPositionAccessor) copy).exotic_worlds$setZ(nearest.z);
        return copy;
    }

    @WrapOperation(
            method = "spreadPositions",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/commands/SpreadPlayersCommand$Position;clamp(DDDD)Z"))
    private static boolean exotic_worlds$confineAcrossSeam(SpreadPlayersCommand.Position position, double minX,
            double minZ, double maxX, double maxZ, Operation<Boolean> original,
            @Share(SPREAD_FOLD) LocalRef<@Nullable WorldFold> spreadFold) {
        WorldFold transformer = spreadFold.get();
        if (transformer == null) {
            return original.call(position, minX, minZ, maxX, maxZ);
        }

        SpreadPositionAccessor accessor = (SpreadPositionAccessor) position;
        SpreadArea.Confined confined = SpreadArea.confine(transformer, accessor.exotic_worlds$x(),
                accessor.exotic_worlds$z(), minX, minZ, maxX, maxZ);
        accessor.exotic_worlds$setX(confined.x());
        accessor.exotic_worlds$setZ(confined.z());
        return confined.clamped();
    }

    @WrapOperation(
            method = "spreadPositions",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/commands/SpreadPlayersCommand$Position;"
                            + "randomize(Lnet/minecraft/util/RandomSource;DDDD)V"))
    private static void exotic_worlds$randomizeAcrossSeam(SpreadPlayersCommand.Position position, RandomSource random,
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
                SpreadArea.randomMin(xAxis, minX, maxX), SpreadArea.randomMin(zAxis, minZ, maxZ),
                SpreadArea.randomMax(xAxis, minX, maxX), SpreadArea.randomMax(zAxis, minZ, maxZ));
    }

    @WrapOperation(
            method = "setPlayerPositions",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/commands/SpreadPlayersCommand$Position;dist(Lnet/minecraft/server/commands/SpreadPlayersCommand$Position;)D"))
    private static double exotic_worlds$distThroughSeam(SpreadPlayersCommand.Position position,
            SpreadPlayersCommand.Position target, Operation<Double> original,
            @Local(argsOnly = true) ServerLevel level) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return original.call(position, target);
        }

        SpreadPositionAccessor from = (SpreadPositionAccessor) position;
        SpreadPositionAccessor to = (SpreadPositionAccessor) target;
        return Math.sqrt(SeamRange.sqr(transformer,
                new Vec3(from.exotic_worlds$x(), 0.0, from.exotic_worlds$z()),
                new Vec3(to.exotic_worlds$x(), 0.0, to.exotic_worlds$z())));
    }
}
