package com.exoticworlds.compat.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.terraforged.SiteLattice;

import net.minecraft.core.Direction;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.landform.SeaCliffs;

@Mixin(value = SeaCliffs.class, remap = false)
public abstract class SeaCliffsMixin {
    @Unique
    private static final float SEA_GRID = 32.0F;

    @Unique
    private static final float STACK_GRID = 40.0F;

    @Unique
    private static final String APPLY_STACK = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/landform/SeaCliffs;"
            + "applyStack(Lcom/pandaismyname1/ultraterraforged/world/worldgen/cell/Cell;FFII"
            + "Lcom/pandaismyname1/ultraterraforged/world/worldgen/heightmap/Heightmap;)V";

    @Unique
    private static final int X_READ = 0;

    @Unique
    private static final int Z_READ = 1;

    @Unique
    private static final int FREE_READ = 0;

    @Unique
    private static final int ORIGIN_X_READ = 1;

    @Unique
    private static final int ORIGIN_Z_READ = 2;

    @Shadow
    private float cornerProximity(int gridX, int gridZ, Heightmap heightmap) {
        throw new AssertionError();
    }

    @WrapMethod(method = "seaProximity")
    private float exotic_worlds$closeSeaOnLap(float x, float z, Heightmap heightmap, Operation<Float> original) {
        SiteLattice lattice = SiteLattice.bound(SEA_GRID);
        if (lattice == null) {
            return original.call(x, z, heightmap);
        }

        return lattice.interpolate(x, z, (cellX, cellZ) -> cornerProximity(cellX, cellZ, heightmap));
    }

    @ModifyConstant(method = "cornerProximity", constant = @Constant(floatValue = SEA_GRID, ordinal = X_READ))
    private float exotic_worlds$seaCornerX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyConstant(method = "cornerProximity", constant = @Constant(floatValue = SEA_GRID, ordinal = Z_READ))
    private float exotic_worlds$seaCornerZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }

    @ModifyConstant(method = "applyStacks", constant = @Constant(floatValue = STACK_GRID, ordinal = X_READ))
    private float exotic_worlds$stackCellX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyConstant(method = "applyStacks", constant = @Constant(floatValue = STACK_GRID, ordinal = Z_READ))
    private float exotic_worlds$stackCellZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }

    @WrapOperation(method = "applyStacks", at = @At(value = "INVOKE", target = APPLY_STACK))
    private void exotic_worlds$seatStack(SeaCliffs cliffs, Cell cell, float x, float z, int gridX, int gridZ,
            Heightmap heightmap, Operation<Void> original, @Local(name = "gridX") int pointX,
            @Local(name = "gridZ") int pointZ) {
        SiteLattice lattice = SiteLattice.bound(STACK_GRID);
        if (lattice == null) {
            original.call(cliffs, cell, x, z, gridX, gridZ, heightmap);
            return;
        }

        SiteLattice.Seat seat = lattice.seat(gridX, gridZ, pointX, pointZ, x, z);
        original.call(cliffs, cell, seat.x(), seat.z(), seat.cellX(), seat.cellZ(), heightmap);
    }

    @ModifyVariable(method = "applyStack", at = @At("STORE"), name = "radius")
    private float exotic_worlds$stackRadius(float radius) {
        return SiteLattice.scaledOn(radius, STACK_GRID);
    }

    @ModifyConstant(method = "applyStack", constant = @Constant(floatValue = STACK_GRID, ordinal = FREE_READ))
    private float exotic_worlds$stackFree(float spacing) {
        return SiteLattice.sharedOn(spacing);
    }

    @ModifyConstant(method = "applyStack", constant = @Constant(floatValue = STACK_GRID, ordinal = ORIGIN_X_READ))
    private float exotic_worlds$stackOriginX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyConstant(method = "applyStack", constant = @Constant(floatValue = STACK_GRID, ordinal = ORIGIN_Z_READ))
    private float exotic_worlds$stackOriginZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }
}
