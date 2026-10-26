package com.exoticworlds.compat.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.terraforged.RtfLap;
import com.exoticworlds.compat.terraforged.SiteLattice;
import com.exoticworlds.compat.ultraterraforged.SiteSeats;
import com.exoticworlds.compat.ultraterraforged.UltraTerraForgedInjectionTargets;

import net.minecraft.core.Direction;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.continent.CoastShaper;
import com.pandaismyname1.ultraterraforged.world.worldgen.landform.Landform;
import com.pandaismyname1.ultraterraforged.world.worldgen.landform.SiteCache;

@Mixin(value = CoastShaper.class, remap = false)
public abstract class CoastShaperMixin {
    @Unique
    private static final float ISLAND_GRID = 640.0F;

    @Unique
    private static final float ISLAND_MARGIN = 160.0F;

    @Unique
    private static final float ISLAND_MIN_RADIUS = 45.0F;

    @Unique
    private static final float PENINSULA_MIN_LENGTH = 500.0F;

    @Unique
    private static final float PENINSULA_MAX_LENGTH = 1400.0F;

    @Unique
    private static final float PENINSULA_MIN_WIDTH = 100.0F;

    @Unique
    private static final float PENINSULA_MAX_WIDTH = 230.0F;

    @Unique
    private static final float SPIT_MIN_LENGTH = 180.0F;

    @Unique
    private static final float SPIT_MAX_LENGTH = 420.0F;

    @Unique
    private static final float ARM_MARGIN_SHARE = 0.2F;

    @Unique
    private static final int ANCHOR_X_PURPOSE = 1;

    @Unique
    private static final int ANCHOR_Z_PURPOSE = 2;

    @Unique
    private static final String FLOOR = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/noise/NoiseUtil;floor(F)I";

    @Unique
    private static final String ARM = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/continent/CoastShaper;"
            + "arm(Lcom/pandaismyname1/ultraterraforged/world/worldgen/continent/CoastShaper$Arm;FFF)F";

    @Unique
    private static final String ISLE = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/continent/CoastShaper;"
            + "isle(Lcom/pandaismyname1/ultraterraforged/world/worldgen/continent/CoastShaper$Isle;FFF[I)F";

    @Unique
    private static final int X_READ = 0;

    @Unique
    private static final int Z_READ = 1;

    @Unique
    private static final int ISLAND_ORIGIN_X_READ = 0;

    @Unique
    private static final int ISLAND_FREE_X_READ = 1;

    @Unique
    private static final int ISLAND_ORIGIN_Z_READ = 2;

    @Unique
    private static final int ISLAND_FREE_Z_READ = 3;

    @Unique
    private static final int SITE_X = 2;

    @Unique
    private static final int SITE_Z = 3;

    @Unique
    private static final int ARM_GRID_ARGUMENT = 3;

    @Unique
    private static final int X_ARGUMENT = 1;

    @Unique
    private static final int Z_ARGUMENT = 2;

    @Unique
    private static final int FIND_GRID_ARGUMENT = 0;

    @Unique
    private static final int FIND_CELL_X_ARGUMENT = 0;

    @Unique
    private static final int FIND_CELL_Z_ARGUMENT = 1;

    @Unique
    private static final int FIND_PURPOSE_ARGUMENT = 2;

    @Shadow
    @Final
    private int seed;

    // In the climate CoastShaper runs inside the biome lattice's octave, whose period would fold its own hash.
    @WrapMethod(method = "apply")
    private void exotic_worlds$ownLattice(Cell cell, float x, float z, Operation<Void> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            original.call(cell, x, z);
            return;
        }

        try (RtfLap.Frame.Scope _ = frame.unperiodic()) {
            original.call(cell, x, z);
        }
    }

    @WrapOperation(method = "arms", at = @At(value = "INVOKE", target = FLOOR, ordinal = X_READ))
    private int exotic_worlds$armCellX(float cell, Operation<Integer> original,
            @Local(argsOnly = true, ordinal = X_ARGUMENT) float x, @Local(argsOnly = true, ordinal = ARM_GRID_ARGUMENT)
            float grid) {
        SiteLattice lattice = SiteLattice.bound(grid);
        return lattice == null ? original.call(cell) : lattice.cellX(x);
    }

    @WrapOperation(method = "arms", at = @At(value = "INVOKE", target = FLOOR, ordinal = Z_READ))
    private int exotic_worlds$armCellZ(float cell, Operation<Integer> original,
            @Local(argsOnly = true, ordinal = Z_ARGUMENT) float z, @Local(argsOnly = true, ordinal = ARM_GRID_ARGUMENT)
            float grid) {
        SiteLattice lattice = SiteLattice.bound(grid);
        return lattice == null ? original.call(cell) : lattice.cellZ(z);
    }

    @WrapOperation(method = "arms", at = @At(value = "INVOKE", target = UltraTerraForgedInjectionTargets.LANDFORM_RANDOM))
    private float exotic_worlds$armHash(int seed, int cellX, int cellZ, int purpose, Operation<Float> original,
            @Local(argsOnly = true, ordinal = X_ARGUMENT) float x, @Local(argsOnly = true, ordinal = Z_ARGUMENT) float z,
            @Local(argsOnly = true, ordinal = ARM_GRID_ARGUMENT) float grid, @Local(name = "gridX") int pointX,
            @Local(name = "gridZ") int pointZ) {
        return SiteSeats.hash(grid, original, seed, cellX, cellZ, purpose, x, z, pointX, pointZ);
    }

    @WrapOperation(method = "arms", at = @At(value = "INVOKE", target = UltraTerraForgedInjectionTargets.SITE_CACHE_GET))
    private Object exotic_worlds$armSite(SiteCache<?> cache, int cellX, int cellZ, SiteCache.Finder<?> finder,
            Operation<Object> original, @Local(argsOnly = true, ordinal = X_ARGUMENT) float x,
            @Local(argsOnly = true, ordinal = Z_ARGUMENT) float z,
            @Local(argsOnly = true, ordinal = ARM_GRID_ARGUMENT) float grid, @Local(name = "gridX") int pointX,
            @Local(name = "gridZ") int pointZ) {
        return SiteSeats.site(grid, original, cache, cellX, cellZ, finder, x, z, pointX, pointZ);
    }

    @ModifyArg(method = "arms", at = @At(value = "INVOKE", target = ARM), index = SITE_X)
    private float exotic_worlds$seatArmX(float seatedX, @Local(argsOnly = true, ordinal = X_ARGUMENT) float x,
            @Local(argsOnly = true, ordinal = Z_ARGUMENT) float z,
            @Local(argsOnly = true, ordinal = ARM_GRID_ARGUMENT) float grid, @Local(name = "gx") int cellX,
            @Local(name = "gz") int cellZ, @Local(name = "gridX") int pointX, @Local(name = "gridZ") int pointZ) {
        SiteLattice.Seat seat = SiteSeats.seat(grid, cellX, cellZ, x, z, pointX, pointZ);
        return seat == null ? seatedX : seat.x();
    }

    @ModifyArg(method = "arms", at = @At(value = "INVOKE", target = ARM), index = SITE_Z)
    private float exotic_worlds$seatArmZ(float seatedZ, @Local(argsOnly = true, ordinal = X_ARGUMENT) float x,
            @Local(argsOnly = true, ordinal = Z_ARGUMENT) float z,
            @Local(argsOnly = true, ordinal = ARM_GRID_ARGUMENT) float grid, @Local(name = "gx") int cellX,
            @Local(name = "gz") int cellZ, @Local(name = "gridX") int pointX, @Local(name = "gridZ") int pointZ) {
        SiteLattice.Seat seat = SiteSeats.seat(grid, cellX, cellZ, x, z, pointX, pointZ);
        return seat == null ? seatedZ : seat.z();
    }

    @ModifyVariable(method = "findArm", at = @At("STORE"), name = "x")
    private float exotic_worlds$armAnchorX(float x, @Local(argsOnly = true, ordinal = FIND_CELL_X_ARGUMENT) int cellX,
            @Local(argsOnly = true, ordinal = FIND_CELL_Z_ARGUMENT) int cellZ, @Local(argsOnly = true, ordinal = FIND_GRID_ARGUMENT)
            float grid, @Local(argsOnly = true, ordinal = FIND_PURPOSE_ARGUMENT) int purpose) {
        SiteLattice lattice = SiteLattice.bound(grid);
        return lattice == null ? x : exotic_worlds$anchor(lattice.xSpacing(), cellX, cellX, cellZ, purpose,
                ANCHOR_X_PURPOSE);
    }

    @ModifyVariable(method = "findArm", at = @At("STORE"), name = "z")
    private float exotic_worlds$armAnchorZ(float z, @Local(argsOnly = true, ordinal = FIND_CELL_X_ARGUMENT) int cellX,
            @Local(argsOnly = true, ordinal = FIND_CELL_Z_ARGUMENT) int cellZ, @Local(argsOnly = true, ordinal = FIND_GRID_ARGUMENT)
            float grid, @Local(argsOnly = true, ordinal = FIND_PURPOSE_ARGUMENT) int purpose) {
        SiteLattice lattice = SiteLattice.bound(grid);
        return lattice == null ? z : exotic_worlds$anchor(lattice.zSpacing(), cellZ, cellX, cellZ, purpose,
                ANCHOR_Z_PURPOSE);
    }

    @ModifyConstant(method = "findArm", constant = {@Constant(floatValue = PENINSULA_MIN_LENGTH),
            @Constant(floatValue = PENINSULA_MAX_LENGTH), @Constant(floatValue = PENINSULA_MIN_WIDTH),
            @Constant(floatValue = PENINSULA_MAX_WIDTH), @Constant(floatValue = SPIT_MIN_LENGTH),
            @Constant(floatValue = SPIT_MAX_LENGTH)})
    private float exotic_worlds$armSize(float length, @Local(argsOnly = true, ordinal = FIND_GRID_ARGUMENT)
            float grid) {
        return SiteLattice.scaledOn(length, grid);
    }

    @ModifyConstant(method = "islands", constant = @Constant(floatValue = ISLAND_GRID, ordinal = X_READ))
    private float exotic_worlds$isleCellX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyConstant(method = "islands", constant = @Constant(floatValue = ISLAND_GRID, ordinal = Z_READ))
    private float exotic_worlds$isleCellZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }

    @WrapOperation(method = "islands", at = @At(value = "INVOKE",
            target = UltraTerraForgedInjectionTargets.LANDFORM_RANDOM))
    private float exotic_worlds$isleHash(int seed, int cellX, int cellZ, int purpose, Operation<Float> original,
            @Local(argsOnly = true, ordinal = X_ARGUMENT) float x, @Local(argsOnly = true, ordinal = Z_ARGUMENT) float z,
            @Local(name = "gridX") int pointX, @Local(name = "gridZ") int pointZ) {
        return SiteSeats.hash(ISLAND_GRID, original, seed, cellX, cellZ, purpose, x, z, pointX, pointZ);
    }

    @WrapOperation(method = "islands", at = @At(value = "INVOKE",
            target = UltraTerraForgedInjectionTargets.SITE_CACHE_GET))
    private Object exotic_worlds$isleSite(SiteCache<?> cache, int cellX, int cellZ, SiteCache.Finder<?> finder,
            Operation<Object> original, @Local(argsOnly = true, ordinal = X_ARGUMENT) float x,
            @Local(argsOnly = true, ordinal = Z_ARGUMENT) float z, @Local(name = "gridX") int pointX,
            @Local(name = "gridZ") int pointZ) {
        return SiteSeats.site(ISLAND_GRID, original, cache, cellX, cellZ, finder, x, z, pointX, pointZ);
    }

    @ModifyArg(method = "islands", at = @At(value = "INVOKE", target = ISLE), index = SITE_X)
    private float exotic_worlds$seatIsleX(float seatedX, @Local(argsOnly = true, ordinal = X_ARGUMENT) float x,
            @Local(argsOnly = true, ordinal = Z_ARGUMENT) float z, @Local(name = "gx") int cellX,
            @Local(name = "gz") int cellZ, @Local(name = "gridX") int pointX, @Local(name = "gridZ") int pointZ) {
        SiteLattice.Seat seat = SiteSeats.seat(ISLAND_GRID, cellX, cellZ, x, z, pointX, pointZ);
        return seat == null ? seatedX : seat.x();
    }

    @ModifyArg(method = "islands", at = @At(value = "INVOKE", target = ISLE), index = SITE_Z)
    private float exotic_worlds$seatIsleZ(float seatedZ, @Local(argsOnly = true, ordinal = X_ARGUMENT) float x,
            @Local(argsOnly = true, ordinal = Z_ARGUMENT) float z, @Local(name = "gx") int cellX,
            @Local(name = "gz") int cellZ, @Local(name = "gridX") int pointX, @Local(name = "gridZ") int pointZ) {
        SiteLattice.Seat seat = SiteSeats.seat(ISLAND_GRID, cellX, cellZ, x, z, pointX, pointZ);
        return seat == null ? seatedZ : seat.z();
    }

    @ModifyConstant(method = "findIsle", constant = {@Constant(floatValue = ISLAND_MARGIN),
            @Constant(floatValue = ISLAND_MIN_RADIUS)})
    private float exotic_worlds$isleSize(float length) {
        return SiteLattice.scaledOn(length, ISLAND_GRID);
    }

    @ModifyConstant(method = "findIsle", constant = @Constant(floatValue = ISLAND_GRID, ordinal = ISLAND_ORIGIN_X_READ))
    private float exotic_worlds$isleOriginX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyConstant(method = "findIsle", constant = @Constant(floatValue = ISLAND_GRID, ordinal = ISLAND_FREE_X_READ))
    private float exotic_worlds$isleFreeX(float spacing) {
        return SiteLattice.sharedOn(spacing);
    }

    @ModifyConstant(method = "findIsle", constant = @Constant(floatValue = ISLAND_GRID, ordinal = ISLAND_ORIGIN_Z_READ))
    private float exotic_worlds$isleOriginZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }

    @ModifyConstant(method = "findIsle", constant = @Constant(floatValue = ISLAND_GRID, ordinal = ISLAND_FREE_Z_READ))
    private float exotic_worlds$isleFreeZ(float spacing) {
        return SiteLattice.sharedOn(spacing);
    }

    @Unique
    private float exotic_worlds$anchor(float spacing, int cell, int cellX, int cellZ, int purpose, int axisPurpose) {
        float margin = spacing * ARM_MARGIN_SHARE;
        return cell * spacing + margin
                + Landform.random(this.seed + purpose, cellX, cellZ, axisPurpose) * (spacing - margin * 2.0F);
    }
}
