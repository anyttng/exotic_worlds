package com.exoticworlds.compat.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.terraforged.SiteLattice;
import com.exoticworlds.compat.ultraterraforged.UltraTerraForgedInjectionTargets;

import net.minecraft.core.Direction;
import com.pandaismyname1.ultraterraforged.world.worldgen.landform.Karst;

@Mixin(value = Karst.class, remap = false)
public abstract class KarstMixin {
    @Unique
    private static final float TOWER_GRID = 44.0F;

    @Unique
    private static final float HOLE_GRID = 72.0F;

    @Unique
    private static final int X_READ = 0;

    @Unique
    private static final int Z_READ = 1;

    @Unique
    private static final int TOWER_ORIGIN_X_READ = 2;

    @Unique
    private static final int TOWER_FREE_X_READ = 3;

    @Unique
    private static final int TOWER_ORIGIN_Z_READ = 4;

    @Unique
    private static final int TOWER_FREE_Z_READ = 5;

    @Unique
    private static final int HOLE_FREE_READ = 2;

    @Unique
    private static final int HOLE_ORIGIN_X_READ = 3;

    @Unique
    private static final int HOLE_ORIGIN_Z_READ = 4;

    @Unique
    private static final int X_ARGUMENT = 0;

    @Unique
    private static final int Z_ARGUMENT = 1;

    @ModifyConstant(method = "tower", constant = @Constant(floatValue = TOWER_GRID, ordinal = X_READ))
    private float exotic_worlds$towerCellX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyConstant(method = "tower", constant = @Constant(floatValue = TOWER_GRID, ordinal = Z_READ))
    private float exotic_worlds$towerCellZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }

    @ModifyConstant(method = "tower", constant = @Constant(floatValue = TOWER_GRID, ordinal = TOWER_ORIGIN_X_READ))
    private float exotic_worlds$towerOriginX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyConstant(method = "tower", constant = @Constant(floatValue = TOWER_GRID, ordinal = TOWER_FREE_X_READ))
    private float exotic_worlds$towerFreeX(float spacing) {
        return SiteLattice.sharedOn(spacing);
    }

    @ModifyConstant(method = "tower", constant = @Constant(floatValue = TOWER_GRID, ordinal = TOWER_ORIGIN_Z_READ))
    private float exotic_worlds$towerOriginZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }

    @ModifyConstant(method = "tower", constant = @Constant(floatValue = TOWER_GRID, ordinal = TOWER_FREE_Z_READ))
    private float exotic_worlds$towerFreeZ(float spacing) {
        return SiteLattice.sharedOn(spacing);
    }

    @WrapOperation(method = "tower", at = @At(value = "INVOKE", target = UltraTerraForgedInjectionTargets.LANDFORM_RANDOM))
    private float exotic_worlds$towerHash(int seed, int cellX, int cellZ, int purpose, Operation<Float> original,
            @Local(argsOnly = true, ordinal = X_ARGUMENT) float x, @Local(argsOnly = true, ordinal = Z_ARGUMENT) float z,
            @Local(name = "gridX") int pointX, @Local(name = "gridZ") int pointZ) {
        SiteLattice lattice = SiteLattice.bound(TOWER_GRID);
        if (lattice == null) {
            return original.call(seed, cellX, cellZ, purpose);
        }

        SiteLattice.Seat seat = lattice.seat(cellX, cellZ, pointX, pointZ, x, z);
        return original.call(seed, seat.cellX(), seat.cellZ(), purpose);
    }

    @ModifyVariable(method = "tower", at = @At("STORE"), name = "radius")
    private float exotic_worlds$towerRadius(float radius) {
        return SiteLattice.scaledOn(radius, TOWER_GRID);
    }

    @ModifyVariable(method = "tower", at = @At("STORE"), name = "centerX")
    private float exotic_worlds$towerCenterX(float centerX, @Local(argsOnly = true, ordinal = X_ARGUMENT) float x,
            @Local(argsOnly = true, ordinal = Z_ARGUMENT) float z, @Local(name = "gridX") int pointX,
            @Local(name = "gridZ") int pointZ, @Local(name = "gx") int cellX, @Local(name = "gz") int cellZ) {
        SiteLattice lattice = SiteLattice.bound(TOWER_GRID);
        if (lattice == null) {
            return centerX;
        }

        SiteLattice.Seat seat = lattice.seat(cellX, cellZ, pointX, pointZ, x, z);
        return centerX + (seat.cellX() - cellX) * lattice.xSpacing() + (x - seat.x());
    }

    @ModifyVariable(method = "tower", at = @At("STORE"), name = "centerZ")
    private float exotic_worlds$towerCenterZ(float centerZ, @Local(argsOnly = true, ordinal = X_ARGUMENT) float x,
            @Local(argsOnly = true, ordinal = Z_ARGUMENT) float z, @Local(name = "gridX") int pointX,
            @Local(name = "gridZ") int pointZ, @Local(name = "gx") int cellX, @Local(name = "gz") int cellZ) {
        SiteLattice lattice = SiteLattice.bound(TOWER_GRID);
        if (lattice == null) {
            return centerZ;
        }

        SiteLattice.Seat seat = lattice.seat(cellX, cellZ, pointX, pointZ, x, z);
        return centerZ + (seat.cellZ() - cellZ) * lattice.zSpacing() + (z - seat.z());
    }

    @ModifyConstant(method = "sinkhole", constant = @Constant(floatValue = HOLE_GRID, ordinal = X_READ))
    private float exotic_worlds$holeCellX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyConstant(method = "sinkhole", constant = @Constant(floatValue = HOLE_GRID, ordinal = Z_READ))
    private float exotic_worlds$holeCellZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }

    @ModifyConstant(method = "sinkhole", constant = @Constant(floatValue = HOLE_GRID, ordinal = HOLE_FREE_READ))
    private float exotic_worlds$holeFree(float spacing) {
        return SiteLattice.sharedOn(spacing);
    }

    @ModifyConstant(method = "sinkhole", constant = @Constant(floatValue = HOLE_GRID, ordinal = HOLE_ORIGIN_X_READ))
    private float exotic_worlds$holeOriginX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyConstant(method = "sinkhole", constant = @Constant(floatValue = HOLE_GRID, ordinal = HOLE_ORIGIN_Z_READ))
    private float exotic_worlds$holeOriginZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }

    @ModifyVariable(method = "sinkhole", at = @At("STORE"), name = "radius")
    private float exotic_worlds$holeRadius(float radius) {
        return SiteLattice.scaledOn(radius, HOLE_GRID);
    }
}
