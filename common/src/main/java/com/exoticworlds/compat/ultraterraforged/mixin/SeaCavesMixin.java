package com.exoticworlds.compat.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
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

@Mixin(targets = "com.pandaismyname1.ultraterraforged.world.worldgen.cave.SeaCaves", remap = false)
public abstract class SeaCavesMixin {
    @Unique
    private static final float GRID = 40.0F;

    @Unique
    private static final int X_READ = 0;

    @Unique
    private static final int Z_READ = 1;

    @Unique
    private static final int FIRST_STORE = 0;

    @Unique
    private static final String CAVE = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/cave/SeaCaves$Cave;"
            + "<init>(FFFFF)V";

    @Unique
    private static final int CAVE_FROM_X = 0;

    @Unique
    private static final int CAVE_FROM_Z = 1;

    @Unique
    private static final int CAVE_TO_X = 2;

    @Unique
    private static final int CAVE_TO_Z = 3;

    @ModifyVariable(method = "carve", at = @At("STORE"), name = "gridX")
    private int exotic_worlds$pointX(int pointX, @Local(argsOnly = true) CaveCarving carving) {
        return CaveCells.pointX(GRID, carving.minX, pointX);
    }

    @ModifyVariable(method = "carve", at = @At("STORE"), name = "gridZ")
    private int exotic_worlds$pointZ(int pointZ, @Local(argsOnly = true) CaveCarving carving) {
        return CaveCells.pointZ(GRID, carving.minZ, pointZ);
    }

    @WrapOperation(method = "carve", at = @At(value = "INVOKE", target = UltraTerraForgedInjectionTargets.CAVE_RANDOM))
    private float exotic_worlds$gate(int seed, int cellX, int cellZ, int purpose, Operation<Float> original,
            @Local(argsOnly = true) CaveCarving carving, @Local(name = "gridX") int pointX) {
        return CaveCells.gate(GRID, original, seed, cellX, cellZ, purpose, carving, pointX);
    }

    @WrapOperation(method = "carve", at = @At(value = "INVOKE", target = UltraTerraForgedInjectionTargets.SITE_CACHE_GET))
    private Object exotic_worlds$site(SiteCache<?> cache, int cellX, int cellZ, SiteCache.Finder<?> finder,
            Operation<Object> original, @Local(argsOnly = true) CaveCarving carving, @Local(name = "gridX") int pointX) {
        return original.call(cache, CaveCells.copyX(GRID, cellX, cellZ, carving, pointX), cellZ,
                CaveCells.seated(GRID, finder));
    }

    @ModifyConstant(method = "find", constant = @Constant(floatValue = GRID, ordinal = X_READ))
    private float exotic_worlds$originX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyConstant(method = "find", constant = @Constant(floatValue = GRID, ordinal = Z_READ))
    private float exotic_worlds$originZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }

    @ModifyVariable(method = "find", at = @At(value = "STORE", ordinal = FIRST_STORE), name = "length")
    private float exotic_worlds$length(float length) {
        return SiteLattice.scaledOn(length, GRID);
    }

    @ModifyArg(method = "find", at = @At(value = "INVOKE", target = CAVE), index = CAVE_FROM_X)
    private float exotic_worlds$seatFromX(float fromX) {
        return CaveCells.seatX(fromX);
    }

    @ModifyArg(method = "find", at = @At(value = "INVOKE", target = CAVE), index = CAVE_FROM_Z)
    private float exotic_worlds$seatFromZ(float fromZ) {
        return CaveCells.seatZ(fromZ);
    }

    @ModifyArg(method = "find", at = @At(value = "INVOKE", target = CAVE), index = CAVE_TO_X)
    private float exotic_worlds$seatToX(float toX) {
        return CaveCells.seatX(toX);
    }

    @ModifyArg(method = "find", at = @At(value = "INVOKE", target = CAVE), index = CAVE_TO_Z)
    private float exotic_worlds$seatToZ(float toZ) {
        return CaveCells.seatZ(toZ);
    }
}
