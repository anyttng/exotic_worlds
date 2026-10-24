package com.exoticworlds.compat.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.terraforged.SiteLattice;
import com.exoticworlds.compat.ultraterraforged.SiteSeats;
import com.exoticworlds.compat.ultraterraforged.UltraTerraForgedInjectionTargets;

import net.minecraft.core.Direction;
import com.pandaismyname1.ultraterraforged.world.worldgen.landform.AlluvialFans;
import com.pandaismyname1.ultraterraforged.world.worldgen.landform.SiteCache;

@Mixin(value = AlluvialFans.class, remap = false)
public abstract class AlluvialFansMixin {
    @Unique
    private static final float GRID = 256.0F;

    @Unique
    private static final float MARGIN = 32.0F;

    @Unique
    private static final String DEPOSIT = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/landform/AlluvialFans;"
            + "deposit(Lcom/pandaismyname1/ultraterraforged/world/worldgen/landform/AlluvialFans$Fan;FFF)F";

    @Unique
    private static final int X_READ = 0;

    @Unique
    private static final int Z_READ = 1;

    @Unique
    private static final int RANGE_READ = 2;

    @Unique
    private static final int FREE_READ = 0;

    @Unique
    private static final int ORIGIN_X_READ = 1;

    @Unique
    private static final int ORIGIN_Z_READ = 2;

    @Unique
    private static final int DEPOSIT_X = 1;

    @Unique
    private static final int DEPOSIT_Z = 2;

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

    @ModifyConstant(method = "apply", constant = @Constant(floatValue = GRID, ordinal = RANGE_READ))
    private float exotic_worlds$range(float spacing) {
        return SiteLattice.sharedOn(spacing);
    }

    @WrapOperation(method = "apply", at = @At(value = "INVOKE", target = UltraTerraForgedInjectionTargets.LANDFORM_RANDOM))
    private float exotic_worlds$hash(int seed, int cellX, int cellZ, int purpose, Operation<Float> original,
            @Local(argsOnly = true, ordinal = X_ARGUMENT) float x, @Local(argsOnly = true, ordinal = Z_ARGUMENT) float z,
            @Local(name = "gridX") int pointX, @Local(name = "gridZ") int pointZ) {
        return SiteSeats.hash(GRID, original, seed, cellX, cellZ, purpose, x, z, pointX, pointZ);
    }

    @WrapOperation(method = "apply", at = @At(value = "INVOKE", target = UltraTerraForgedInjectionTargets.SITE_CACHE_GET))
    private Object exotic_worlds$site(SiteCache<?> cache, int cellX, int cellZ, SiteCache.Finder<?> finder,
            Operation<Object> original, @Local(argsOnly = true, ordinal = X_ARGUMENT) float x,
            @Local(argsOnly = true, ordinal = Z_ARGUMENT) float z, @Local(name = "gridX") int pointX,
            @Local(name = "gridZ") int pointZ) {
        return SiteSeats.site(GRID, original, cache, cellX, cellZ, finder, x, z, pointX, pointZ);
    }

    @ModifyArg(method = "apply", at = @At(value = "INVOKE", target = DEPOSIT), index = DEPOSIT_X)
    private float exotic_worlds$seatDepositX(float seatedX, @Local(argsOnly = true, ordinal = X_ARGUMENT) float x,
            @Local(argsOnly = true, ordinal = Z_ARGUMENT) float z, @Local(name = "gx") int cellX,
            @Local(name = "gz") int cellZ, @Local(name = "gridX") int pointX, @Local(name = "gridZ") int pointZ) {
        SiteLattice.Seat seat = SiteSeats.seat(GRID, cellX, cellZ, x, z, pointX, pointZ);
        return seat == null ? seatedX : seat.x();
    }

    @ModifyArg(method = "apply", at = @At(value = "INVOKE", target = DEPOSIT), index = DEPOSIT_Z)
    private float exotic_worlds$seatDepositZ(float seatedZ, @Local(argsOnly = true, ordinal = X_ARGUMENT) float x,
            @Local(argsOnly = true, ordinal = Z_ARGUMENT) float z, @Local(name = "gx") int cellX,
            @Local(name = "gz") int cellZ, @Local(name = "gridX") int pointX, @Local(name = "gridZ") int pointZ) {
        SiteLattice.Seat seat = SiteSeats.seat(GRID, cellX, cellZ, x, z, pointX, pointZ);
        return seat == null ? seatedZ : seat.z();
    }

    @ModifyConstant(method = "find", constant = @Constant(floatValue = MARGIN))
    private float exotic_worlds$margin(float margin) {
        return SiteLattice.scaledOn(margin, GRID);
    }

    @ModifyConstant(method = "find", constant = @Constant(floatValue = GRID, ordinal = FREE_READ))
    private float exotic_worlds$free(float spacing) {
        return SiteLattice.sharedOn(spacing);
    }

    @ModifyConstant(method = "find", constant = @Constant(floatValue = GRID, ordinal = ORIGIN_X_READ))
    private float exotic_worlds$originX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyConstant(method = "find", constant = @Constant(floatValue = GRID, ordinal = ORIGIN_Z_READ))
    private float exotic_worlds$originZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }
}
