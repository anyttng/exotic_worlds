package com.exoticworlds.engine.fold;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.FoldHolder;
import com.exoticworlds.core.WorldFold;

import net.minecraft.util.Mth;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public record CuboidBorder(WorldFold fold, double minX, double maxX, double minZ, double maxZ) implements SeamBorder {
    public static @Nullable CuboidBorder of(WorldBorder border) {
        WorldFold fold = ((FoldHolder) border).exotic_worlds$fold();
        return fold.cuboidAtlas() == null
                ? null
                : new CuboidBorder(fold, border.getMinX(), border.getMaxX(), border.getMinZ(), border.getMaxZ());
    }

    @Override
    public boolean inside(double x, double z, double margin) {
        AABB box = seatedFor(x, z);
        return x >= box.minX - margin && x < box.maxX + margin && z >= box.minZ - margin && z < box.maxZ + margin;
    }

    @Override
    public double distanceToEdge(double x, double z) {
        AABB box = seatedFor(x, z);
        return Math.min(Math.min(x - box.minX, box.maxX - x), Math.min(z - box.minZ, box.maxZ - z));
    }

    @Override
    public Vec3 clamped(double x, double y, double z) {
        AABB box = seatedFor(x, z);
        return new Vec3(
                Mth.clamp(x, box.minX, box.maxX - CLAMP_INSET),
                y,
                Mth.clamp(z, box.minZ, box.maxZ - CLAMP_INSET));
    }

    public VoxelShape wallAround(double x, double z) {
        AABB box = seatedFor(x, z);
        return Shapes.join(Shapes.INFINITY, Shapes.box(
                Math.floor(box.minX), Double.NEGATIVE_INFINITY, Math.floor(box.minZ),
                Math.ceil(box.maxX), Double.POSITIVE_INFINITY, Math.ceil(box.maxZ)),
                BooleanOp.ONLY_FIRST);
    }

    private AABB seatedFor(double x, double z) {
        AABB border = new AABB(this.minX, 0.0, this.minZ, this.maxX, 0.0, this.maxZ);
        return this.fold.nearestCopyOriented(new Vec3(x, 0.0, z), border).value();
    }
}
