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

@Mixin(targets = "com.pandaismyname1.ultraterraforged.world.worldgen.cave.KarstCaves", remap = false)
public abstract class KarstCavesMixin {
    @Unique
    private static final float GRID = 80.0F;

    @Unique
    private static final String CHAMBER = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/cave/KarstCaves;"
            + "chamber(IILcom/pandaismyname1/ultraterraforged/world/worldgen/cave/CaveCarving;)"
            + "Lcom/pandaismyname1/ultraterraforged/world/worldgen/cave/KarstCaves$Chamber;";

    @Unique
    private static final int X_READ = 0;

    @Unique
    private static final int Z_READ = 1;

    @Unique
    private static final int ORIGIN_X_READ = 0;

    @Unique
    private static final int FREE_X_READ = 1;

    @Unique
    private static final int ORIGIN_Z_READ = 2;

    @Unique
    private static final int FREE_Z_READ = 3;

    @Unique
    private static final int BASE_CHAMBER = 0;

    @Unique
    private static final int NEXT_CHAMBER = 1;

    @Unique
    private static final int CELL_X = 0;

    @Unique
    private static final int Z_ARGUMENT = 1;

    @ModifyConstant(method = "carve", constant = @Constant(floatValue = GRID, ordinal = X_READ))
    private float exotic_worlds$cellX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyConstant(method = "carve", constant = @Constant(floatValue = GRID, ordinal = Z_READ))
    private float exotic_worlds$cellZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }

    @ModifyArg(method = "carve", at = @At(value = "INVOKE", target = CHAMBER, ordinal = BASE_CHAMBER), index = CELL_X)
    private int exotic_worlds$baseCopy(int cellX, @Local(name = "gz") int cellZ,
            @Local(argsOnly = true) CaveCarving carving, @Local(name = "gridX") int pointX) {
        return CaveCells.copyX(GRID, cellX, cellZ, carving, pointX);
    }

    @ModifyArg(method = "carve", at = @At(value = "INVOKE", target = CHAMBER, ordinal = NEXT_CHAMBER), index = CELL_X)
    private int exotic_worlds$nextCopy(int nextX, @Local(name = "gx") int cellX, @Local(name = "gz") int cellZ,
            @Local(argsOnly = true) CaveCarving carving, @Local(name = "gridX") int pointX) {
        return CaveCells.copyX(GRID, cellX, cellZ, carving, pointX) + nextX - cellX;
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

    @WrapOperation(method = "find", at = @At(value = "INVOKE", target = UltraTerraForgedInjectionTargets.CAVE_RANDOM))
    private float exotic_worlds$hash(int seed, int copyX, int copyZ, int purpose, Operation<Float> original) {
        return CaveCells.hash(GRID, original, seed, copyX, copyZ, purpose);
    }

    @ModifyVariable(method = "find", at = @At("STORE"), name = "x")
    private float exotic_worlds$skewOrigin(float x, @Local(argsOnly = true, ordinal = Z_ARGUMENT) int copyZ) {
        return CaveCells.originX(GRID, x, copyZ);
    }

    @ModifyVariable(method = "find", at = @At("STORE"), name = "radius")
    private float exotic_worlds$radius(float radius) {
        return SiteLattice.scaledOn(radius, GRID);
    }
}
