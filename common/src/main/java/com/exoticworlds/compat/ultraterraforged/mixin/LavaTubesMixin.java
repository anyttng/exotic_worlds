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

@Mixin(targets = "com.pandaismyname1.ultraterraforged.world.worldgen.cave.LavaTubes", remap = false)
public abstract class LavaTubesMixin {
    @Unique
    private static final float GRID = 320.0F;

    @Unique
    private static final float STEP = 10.0F;

    @Unique
    private static final int X_READ = 0;

    @Unique
    private static final int Z_READ = 1;

    @Unique
    private static final String TUBE = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/cave/LavaTubes$Tube;"
            + "<init>([F[F[F[ZFFFF)V";

    @Unique
    private static final int TUBE_XS = 0;

    @Unique
    private static final int TUBE_ZS = 2;

    @Unique
    private static final int TUBE_MIN_X = 4;

    @Unique
    private static final int TUBE_MIN_Z = 5;

    @Unique
    private static final int TUBE_MAX_X = 6;

    @Unique
    private static final int TUBE_MAX_Z = 7;

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

    @ModifyConstant(method = "find", constant = @Constant(floatValue = STEP))
    private float exotic_worlds$step(float step) {
        return SiteLattice.scaledOn(step, GRID);
    }

    @ModifyArg(method = "find", at = @At(value = "INVOKE", target = TUBE), index = TUBE_XS)
    private float[] exotic_worlds$seatXs(float[] xs) {
        return CaveCells.seatXs(xs);
    }

    @ModifyArg(method = "find", at = @At(value = "INVOKE", target = TUBE), index = TUBE_ZS)
    private float[] exotic_worlds$seatZs(float[] zs) {
        return CaveCells.seatZs(zs);
    }

    @ModifyArg(method = "find", at = @At(value = "INVOKE", target = TUBE), index = TUBE_MIN_X)
    private float exotic_worlds$seatMinX(float minX) {
        return CaveCells.seatX(minX);
    }

    @ModifyArg(method = "find", at = @At(value = "INVOKE", target = TUBE), index = TUBE_MIN_Z)
    private float exotic_worlds$seatMinZ(float minZ) {
        return CaveCells.seatZ(minZ);
    }

    @ModifyArg(method = "find", at = @At(value = "INVOKE", target = TUBE), index = TUBE_MAX_X)
    private float exotic_worlds$seatMaxX(float maxX) {
        return CaveCells.seatX(maxX);
    }

    @ModifyArg(method = "find", at = @At(value = "INVOKE", target = TUBE), index = TUBE_MAX_Z)
    private float exotic_worlds$seatMaxZ(float maxZ) {
        return CaveCells.seatZ(maxZ);
    }
}
