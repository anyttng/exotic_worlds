package com.exoticworlds.compat.sable;

import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.core.JomlVectors;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

public final class SableSeamDistance {
    public static double rectilinear(Level level, Vector3dc from, Vector3dc to, Operation<Double> original) {
        return rectilinear(WorldLoopAttachments.wrappedFoldOfReader(level), from, to, original);
    }

    public static double rectilinear(@Nullable WorldFold fold, Vector3dc from, Vector3dc to, Operation<Double> original) {
        if (fold == null) {
            return original.call(from, to);
        }

        Vec3 delta = fold.foldDelta(JomlVectors.read(from), JomlVectors.read(to));
        return Math.max(Math.abs(delta.x), Math.max(Math.abs(delta.y), Math.abs(delta.z)));
    }

    public static double sqr(@Nullable LevelReader level, Vector3dc from, Vector3dc to, Operation<Double> original) {
        return sqr(WorldLoopAttachments.wrappedFoldOfReader(level), from, to, original);
    }

    public static double sqr(@Nullable LevelReader level, Vector3dc receiver, double x, double y, double z,
            Operation<Double> original) {
        return sqr(WorldLoopAttachments.wrappedFoldOfReader(level), receiver,
                receiver.x(), receiver.y(), receiver.z(), x, y, z, original);
    }

    public static double sqr(@Nullable LevelReader level, Vec3 receiver, double x, double y, double z,
            Operation<Double> original) {
        return sqr(WorldLoopAttachments.wrappedFoldOfReader(level), receiver,
                receiver.x, receiver.y, receiver.z, x, y, z, original);
    }

    static double sqr(@Nullable WorldFold fold, Vector3dc from, Vector3dc to, Operation<Double> original) {
        return fold == null
                ? original.call(from, to)
                : fold.sqrDistance(from.x(), from.y(), from.z(), to.x(), to.y(), to.z());
    }

    static double sqr(@Nullable WorldFold fold, Object receiver, double receiverX, double receiverY, double receiverZ,
            double x, double y, double z, Operation<Double> original) {
        return fold == null
                ? original.call(receiver, x, y, z)
                : fold.sqrDistance(receiverX, receiverY, receiverZ, x, y, z);
    }

    private SableSeamDistance() {
    }
}
