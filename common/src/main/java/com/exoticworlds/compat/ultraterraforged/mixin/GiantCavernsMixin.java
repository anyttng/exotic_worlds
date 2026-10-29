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

@Mixin(targets = "com.pandaismyname1.ultraterraforged.world.worldgen.cave.GiantCaverns", remap = false)
public abstract class GiantCavernsMixin {
    @Unique
    private static final float GRID = 900.0F;

    @Unique
    private static final int ORIGIN_X_READ = 0;

    @Unique
    private static final int FREE_X_READ = 1;

    @Unique
    private static final int ORIGIN_Z_READ = 2;

    @Unique
    private static final int FREE_Z_READ = 3;

    @Unique
    private static final String CAVERN = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/cave/GiantCaverns$Cavern;"
            + "<init>(FFFFFF)V";

    @Unique
    private static final int CAVERN_X = 0;

    @Unique
    private static final int CAVERN_Z = 2;

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

    @ModifyConstant(method = "find", constant = @Constant(floatValue = GRID, ordinal = ORIGIN_X_READ))
    private float exotic_worlds$originX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyConstant(method = "find", constant = @Constant(floatValue = GRID, ordinal = FREE_X_READ))
    private float exotic_worlds$freeX(float spacing) {
        return SiteLattice.sharedOn(spacing);
    }

    @ModifyConstant(method = "find", constant = @Constant(floatValue = GRID, ordinal = ORIGIN_Z_READ))
    private float exotic_worlds$originZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }

    @ModifyConstant(method = "find", constant = @Constant(floatValue = GRID, ordinal = FREE_Z_READ))
    private float exotic_worlds$freeZ(float spacing) {
        return SiteLattice.sharedOn(spacing);
    }

    @ModifyVariable(method = "find", at = @At("STORE"), name = "margin")
    private float exotic_worlds$margin(float margin) {
        return SiteLattice.scaledOn(margin, GRID);
    }

    @ModifyVariable(method = "find", at = @At("STORE"), name = "radiusX")
    private float exotic_worlds$radiusX(float radius) {
        return SiteLattice.scaledOn(radius, GRID);
    }

    @ModifyVariable(method = "find", at = @At("STORE"), name = "radiusZ")
    private float exotic_worlds$radiusZ(float radius) {
        return SiteLattice.scaledOn(radius, GRID);
    }

    @ModifyArg(method = "find", at = @At(value = "INVOKE", target = CAVERN), index = CAVERN_X)
    private float exotic_worlds$seatX(float x) {
        return CaveCells.seatX(x);
    }

    @ModifyArg(method = "find", at = @At(value = "INVOKE", target = CAVERN), index = CAVERN_Z)
    private float exotic_worlds$seatZ(float z) {
        return CaveCells.seatZ(z);
    }
}
