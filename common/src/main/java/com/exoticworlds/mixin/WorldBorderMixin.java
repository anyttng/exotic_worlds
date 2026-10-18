package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.TransformerHolder;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.engine.fold.SeamBorder;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

@Mixin(WorldBorder.class)
public class WorldBorderMixin implements TransformerHolder {
    @Unique
    private WorldFold exotic_worlds$transformer = WorldFolds.NOOP;

    @Unique
    private double @Nullable [] exotic_worlds$wallBounds;

    @Unique
    private @Nullable VoxelShape exotic_worlds$wall;

    @Shadow
    public double getMinX() {
        throw new AssertionError();
    }

    @Shadow
    public double getMaxX() {
        throw new AssertionError();
    }

    @Shadow
    public double getMinZ() {
        throw new AssertionError();
    }

    @Shadow
    public double getMaxZ() {
        throw new AssertionError();
    }

    @Override
    public WorldFold exotic_worlds$transformer() {
        return this.exotic_worlds$transformer;
    }

    @Override
    public void exotic_worlds$setTransformer(WorldFold transformer) {
        this.exotic_worlds$transformer = transformer;
    }

    @ModifyReturnValue(method = "isWithinBounds(DDD)Z", at = @At("RETURN"))
    private boolean exotic_worlds$boundsThroughSeam(boolean original, double x, double z, double margin) {
        WorldFold transformer = this.exotic_worlds$transformer;
        return transformer.isWrapped() && !exotic_worlds$isForeign(transformer, x, z)
                ? exotic_worlds$border(transformer).inside(x, z, margin)
                : original;
    }

    @ModifyReturnValue(method = "getDistanceToBorder(DD)D", at = @At("RETURN"))
    private double exotic_worlds$distanceThroughSeam(double original, double x, double z) {
        WorldFold transformer = this.exotic_worlds$transformer;
        return transformer.isWrapped() && !exotic_worlds$isForeign(transformer, x, z)
                ? exotic_worlds$border(transformer).distanceToEdge(x, z)
                : original;
    }

    @ModifyReturnValue(method = "clampToBounds(DDD)Lnet/minecraft/core/BlockPos;", at = @At("RETURN"))
    private BlockPos exotic_worlds$clampThroughSeam(BlockPos original, double x, double y, double z) {
        WorldFold transformer = this.exotic_worlds$transformer;
        return transformer.isWrapped() && !exotic_worlds$isForeign(transformer, x, z)
                ? BlockPos.containing(transformer.fold(exotic_worlds$border(transformer).clamped(x, y, z)))
                : original;
    }

    @WrapMethod(method = "getCollisionShape")
    private VoxelShape exotic_worlds$wallThroughSeam(Operation<VoxelShape> original) {
        WorldFold transformer = this.exotic_worlds$transformer;
        if (!transformer.isWrapped()) {
            return original.call();
        }

        double minX = getMinX();
        double maxX = getMaxX();
        double minZ = getMinZ();
        double maxZ = getMaxZ();

        double[] bounds = this.exotic_worlds$wallBounds;
        VoxelShape wall = this.exotic_worlds$wall;
        if (wall == null || bounds == null
                || bounds[0] != minX || bounds[1] != maxX || bounds[2] != minZ || bounds[3] != maxZ) {
            wall = exotic_worlds$buildWall(exotic_worlds$border(transformer));
            this.exotic_worlds$wall = wall;
            this.exotic_worlds$wallBounds = new double[] {minX, maxX, minZ, maxZ};
        }

        return wall;
    }

    @Unique
    private static boolean exotic_worlds$isForeign(WorldFold transformer, double x, double z) {
        TranslationLattice lattice = transformer.blockLattice();
        return lattice.x().isForeign(x) || lattice.z().isForeign(z);
    }

    @Unique
    private SeamBorder exotic_worlds$border(WorldFold transformer) {
        return new SeamBorder(transformer.blockLattice(), getMinX(), getMaxX(), getMinZ(), getMaxZ());
    }

    @Unique
    private static VoxelShape exotic_worlds$buildWall(SeamBorder border) {
        VoxelShape wall = Shapes.INFINITY;
        for (Vec3 shift : border.wallShifts()) {
            wall = Shapes.join(wall, Shapes.box(
                    Math.floor(border.minX() + shift.x), Double.NEGATIVE_INFINITY, Math.floor(border.minZ() + shift.z),
                    Math.ceil(border.maxX() + shift.x), Double.POSITIVE_INFINITY, Math.ceil(border.maxZ() + shift.z)),
                    BooleanOp.ONLY_FIRST);
        }

        return wall;
    }
}
