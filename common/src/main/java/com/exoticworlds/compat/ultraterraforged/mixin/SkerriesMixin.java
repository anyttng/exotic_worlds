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
import com.exoticworlds.compat.ultraterraforged.SiteSeats;
import com.exoticworlds.compat.ultraterraforged.UltraTerraForgedInjectionTargets;

import net.minecraft.core.Direction;
import com.pandaismyname1.ultraterraforged.world.worldgen.landform.Skerries;

@Mixin(value = Skerries.class, remap = false)
public abstract class SkerriesMixin {
    @Unique
    private static final float GRID = 20.0F;

    @Unique
    private static final int X_READ = 0;

    @Unique
    private static final int Z_READ = 1;

    @Unique
    private static final int ORIGIN_X_READ = 2;

    @Unique
    private static final int FREE_X_READ = 3;

    @Unique
    private static final int ORIGIN_Z_READ = 4;

    @Unique
    private static final int FREE_Z_READ = 5;

    @Unique
    private static final int X_ARGUMENT = 0;

    @Unique
    private static final int Z_ARGUMENT = 1;

    @ModifyConstant(method = "apply", constant = @Constant(floatValue = GRID, ordinal = X_READ))
    private float exotic_worlds$cellX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyConstant(method = "apply", constant = @Constant(floatValue = GRID, ordinal = Z_READ))
    private float exotic_worlds$cellZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }

    @ModifyConstant(method = "apply", constant = @Constant(floatValue = GRID, ordinal = ORIGIN_X_READ))
    private float exotic_worlds$originX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyConstant(method = "apply", constant = @Constant(floatValue = GRID, ordinal = FREE_X_READ))
    private float exotic_worlds$freeX(float spacing) {
        return SiteLattice.sharedOn(spacing);
    }

    @ModifyConstant(method = "apply", constant = @Constant(floatValue = GRID, ordinal = ORIGIN_Z_READ))
    private float exotic_worlds$originZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }

    @ModifyConstant(method = "apply", constant = @Constant(floatValue = GRID, ordinal = FREE_Z_READ))
    private float exotic_worlds$freeZ(float spacing) {
        return SiteLattice.sharedOn(spacing);
    }

    @WrapOperation(method = "apply", at = @At(value = "INVOKE", target = UltraTerraForgedInjectionTargets.LANDFORM_RANDOM))
    private float exotic_worlds$hash(int seed, int cellX, int cellZ, int purpose, Operation<Float> original,
            @Local(argsOnly = true, ordinal = X_ARGUMENT) float x, @Local(argsOnly = true, ordinal = Z_ARGUMENT) float z,
            @Local(name = "gridX") int pointX, @Local(name = "gridZ") int pointZ) {
        return SiteSeats.hash(GRID, original, seed, cellX, cellZ, purpose, x, z, pointX, pointZ);
    }

    @ModifyVariable(method = "apply", at = @At("STORE"), name = "radius")
    private float exotic_worlds$radius(float radius) {
        return SiteLattice.scaledOn(radius, GRID);
    }

    @ModifyVariable(method = "apply", at = @At("STORE"), name = "centerX")
    private float exotic_worlds$centerX(float centerX, @Local(argsOnly = true, ordinal = X_ARGUMENT) float x,
            @Local(argsOnly = true, ordinal = Z_ARGUMENT) float z, @Local(name = "gridX") int pointX,
            @Local(name = "gridZ") int pointZ, @Local(name = "gx") int cellX, @Local(name = "gz") int cellZ) {
        SiteLattice lattice = SiteLattice.bound(GRID);
        if (lattice == null) {
            return centerX;
        }

        SiteLattice.Seat seat = lattice.seat(cellX, cellZ, pointX, pointZ, x, z);
        return centerX + (seat.cellX() - cellX) * lattice.xSpacing() + (x - seat.x());
    }

    @ModifyVariable(method = "apply", at = @At("STORE"), name = "centerZ")
    private float exotic_worlds$centerZ(float centerZ, @Local(argsOnly = true, ordinal = X_ARGUMENT) float x,
            @Local(argsOnly = true, ordinal = Z_ARGUMENT) float z, @Local(name = "gridX") int pointX,
            @Local(name = "gridZ") int pointZ, @Local(name = "gx") int cellX, @Local(name = "gz") int cellZ) {
        SiteLattice lattice = SiteLattice.bound(GRID);
        if (lattice == null) {
            return centerZ;
        }

        SiteLattice.Seat seat = lattice.seat(cellX, cellZ, pointX, pointZ, x, z);
        return centerZ + (seat.cellZ() - cellZ) * lattice.zSpacing() + (z - seat.z());
    }
}
