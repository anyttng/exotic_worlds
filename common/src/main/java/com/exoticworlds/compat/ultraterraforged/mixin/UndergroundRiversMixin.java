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
import com.exoticworlds.compat.ultraterraforged.CaveCells;
import com.exoticworlds.compat.ultraterraforged.UltraTerraForgedInjectionTargets;

import net.minecraft.core.Direction;
import com.pandaismyname1.ultraterraforged.world.worldgen.cave.CaveCarving;
import com.pandaismyname1.ultraterraforged.world.worldgen.landform.SiteCache;

@Mixin(targets = "com.pandaismyname1.ultraterraforged.world.worldgen.cave.UndergroundRivers", remap = false)
public abstract class UndergroundRiversMixin {
    @Unique
    private static final float GRID = 384.0F;

    @Unique
    private static final float STEP = 22.0F;

    @Unique
    private static final int X_READ = 0;

    @Unique
    private static final int Z_READ = 1;

    @Unique
    private static final int Z_ARGUMENT = 1;

    @Unique
    private static final int FIRST_STORE = 0;

    @ModifyConstant(method = "carve", constant = @Constant(floatValue = GRID, ordinal = X_READ))
    private float exotic_worlds$cellX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyConstant(method = "carve", constant = @Constant(floatValue = GRID, ordinal = Z_READ))
    private float exotic_worlds$cellZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }

    @WrapOperation(method = "carve", at = @At(value = "INVOKE", target = UltraTerraForgedInjectionTargets.CAVE_RANDOM))
    private float exotic_worlds$gate(int seed, int cellX, int cellZ, int purpose, Operation<Float> original,
            @Local(argsOnly = true) CaveCarving carving, @Local(name = "gridX") int pointX) {
        return CaveCells.gate(GRID, original, seed, cellX, cellZ, purpose, carving, pointX);
    }

    @WrapOperation(method = "carve", at = @At(value = "INVOKE", target = UltraTerraForgedInjectionTargets.SITE_CACHE_GET))
    private Object exotic_worlds$site(SiteCache<?> cache, int cellX, int cellZ, SiteCache.Finder<?> finder,
            Operation<Object> original, @Local(argsOnly = true) CaveCarving carving, @Local(name = "gridX") int pointX) {
        return CaveCells.site(GRID, original, cache, cellX, cellZ, finder, carving, pointX);
    }

    @ModifyConstant(method = "find", constant = @Constant(floatValue = GRID, ordinal = X_READ))
    private float exotic_worlds$originX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyConstant(method = "find", constant = @Constant(floatValue = GRID, ordinal = Z_READ))
    private float exotic_worlds$originZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }

    @WrapOperation(method = "find", at = @At(value = "INVOKE", target = UltraTerraForgedInjectionTargets.CAVE_RANDOM))
    private float exotic_worlds$hash(int seed, int copyX, int copyZ, int purpose, Operation<Float> original) {
        return CaveCells.hash(GRID, original, seed, copyX, copyZ, purpose);
    }

    @ModifyVariable(method = "find", at = @At(value = "STORE", ordinal = FIRST_STORE), name = "x")
    private float exotic_worlds$skewOrigin(float x, @Local(argsOnly = true, ordinal = Z_ARGUMENT) int copyZ) {
        return CaveCells.originX(GRID, x, copyZ);
    }

    @ModifyConstant(method = "find", constant = @Constant(floatValue = STEP))
    private float exotic_worlds$step(float step) {
        return SiteLattice.scaledOn(step, GRID);
    }
}
