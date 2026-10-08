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
    private WorldFold toroidal$transformer = WorldFolds.NOOP;

    @Unique
    private double @Nullable [] toroidal$wallBounds;

    @Unique
    private @Nullable VoxelShape toroidal$wall;

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
    public WorldFold toroidal$transformer() {
        return this.toroidal$transformer;
    }

    @Override
    public void toroidal$setTransformer(WorldFold transformer) {
        this.toroidal$transformer = transformer;
    }

    @ModifyReturnValue(method = "isWithinBounds(DDD)Z", at = @At("RETURN"))
    private boolean toroidal$boundsThroughSeam(boolean original, double x, double z, double margin) {
        WorldFold transformer = this.toroidal$transformer;
        return transformer.isWrapped() && !toroidal$isForeign(transformer, x, z)
                ? toroidal$border(transformer).inside(x, z, margin)
                : original;
    }

    @ModifyReturnValue(method = "getDistanceToBorder(DD)D", at = @At("RETURN"))
    private double toroidal$distanceThroughSeam(double original, double x, double z) {
        WorldFold transformer = this.toroidal$transformer;
        return transformer.isWrapped() && !toroidal$isForeign(transformer, x, z)
                ? toroidal$border(transformer).distanceToEdge(x, z)
                : original;
    }

    @ModifyReturnValue(method = "clampToBounds(DDD)Lnet/minecraft/core/BlockPos;", at = @At("RETURN"))
    private BlockPos toroidal$clampThroughSeam(BlockPos original, double x, double y, double z) {
        WorldFold transformer = this.toroidal$transformer;
        return transformer.isWrapped() && !toroidal$isForeign(transformer, x, z)
                ? BlockPos.containing(transformer.fold(toroidal$border(transformer).clamped(x, y, z)))
                : original;
    }

    @WrapMethod(method = "getCollisionShape")
    private VoxelShape toroidal$wallThroughSeam(Operation<VoxelShape> original) {
        WorldFold transformer = this.toroidal$transformer;
        if (!transformer.isWrapped()) {
            return original.call();
        }

        double minX = getMinX();
        double maxX = getMaxX();
        double minZ = getMinZ();
        double maxZ = getMaxZ();

        double[] bounds = this.toroidal$wallBounds;
        VoxelShape wall = this.toroidal$wall;
        if (wall == null || bounds == null
                || bounds[0] != minX || bounds[1] != maxX || bounds[2] != minZ || bounds[3] != maxZ) {
            wall = toroidal$buildWall(toroidal$border(transformer));
            this.toroidal$wall = wall;
            this.toroidal$wallBounds = new double[] {minX, maxX, minZ, maxZ};
        }

        return wall;
    }

    @Unique
    private static boolean toroidal$isForeign(WorldFold transformer, double x, double z) {
        TranslationLattice lattice = transformer.blockLattice();
        return lattice.x().isForeign(x) || lattice.z().isForeign(z);
    }

    @Unique
    private SeamBorder toroidal$border(WorldFold transformer) {
        return new SeamBorder(transformer.blockLattice(), getMinX(), getMaxX(), getMinZ(), getMaxZ());
    }

    @Unique
    private static VoxelShape toroidal$buildWall(SeamBorder border) {
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
