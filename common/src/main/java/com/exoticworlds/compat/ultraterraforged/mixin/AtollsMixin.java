package com.exoticworlds.compat.ultraterraforged.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.terraforged.SiteLattice;
import com.exoticworlds.compat.ultraterraforged.SiteSeats;

import net.minecraft.core.Direction;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.landform.Atolls;

@Mixin(value = Atolls.class, remap = false)
public abstract class AtollsMixin {
    @Unique
    private static final String GRID_SIZE = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/landform/Atolls;gridSize:F";

    @Unique
    private static final String RANDOM = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/landform/Atolls;"
            + "random(IIII)F";

    @Unique
    private static final String APPLY_ATOLL = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/landform/Atolls;"
            + "applyAtoll(Lcom/pandaismyname1/ultraterraforged/world/worldgen/cell/Cell;FFFFII"
            + "Lcom/pandaismyname1/ultraterraforged/world/worldgen/heightmap/Heightmap;)V";

    @Unique
    private static final int X_READ = 0;

    @Unique
    private static final int Z_READ = 1;

    @Unique
    private static final int FREE_READ = 1;

    @Unique
    private static final int ORIGIN_X_READ = 2;

    @Unique
    private static final int ORIGIN_Z_READ = 3;

    @Unique
    private static final int X_ARGUMENT = 0;

    @Unique
    private static final int Z_ARGUMENT = 1;

    @Shadow
    @Final
    private float gridSize;

    @ModifyExpressionValue(method = "apply",
            at = @At(value = "FIELD", target = GRID_SIZE, opcode = Opcodes.GETFIELD, ordinal = X_READ))
    private float exotic_worlds$cellX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyExpressionValue(method = "apply",
            at = @At(value = "FIELD", target = GRID_SIZE, opcode = Opcodes.GETFIELD, ordinal = Z_READ))
    private float exotic_worlds$cellZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }

    @WrapOperation(method = "apply", at = @At(value = "INVOKE", target = RANDOM))
    private float exotic_worlds$hash(int seed, int cellX, int cellZ, int purpose, Operation<Float> original,
            @Local(argsOnly = true, ordinal = X_ARGUMENT) float x, @Local(argsOnly = true, ordinal = Z_ARGUMENT) float z,
            @Local(name = "gridX") int pointX, @Local(name = "gridZ") int pointZ) {
        return SiteSeats.hash(this.gridSize, original, seed, cellX, cellZ, purpose, x, z, pointX, pointZ);
    }

    @WrapOperation(method = "apply", at = @At(value = "INVOKE", target = APPLY_ATOLL))
    private void exotic_worlds$seatAtoll(Atolls atolls, Cell cell, float x, float z, float warpX, float warpZ,
            int cellX, int cellZ, Heightmap heightmap, Operation<Void> original, @Local(name = "gridX") int pointX,
            @Local(name = "gridZ") int pointZ) {
        SiteLattice.Seat seat = SiteSeats.seat(this.gridSize, cellX, cellZ, x, z, pointX, pointZ);
        if (seat == null) {
            original.call(atolls, cell, x, z, warpX, warpZ, cellX, cellZ, heightmap);
            return;
        }

        original.call(atolls, cell, seat.x(), seat.z(), warpX, warpZ, seat.cellX(), seat.cellZ(), heightmap);
    }

    @ModifyExpressionValue(method = "applyAtoll",
            at = @At(value = "FIELD", target = GRID_SIZE, opcode = Opcodes.GETFIELD, ordinal = FREE_READ))
    private float exotic_worlds$free(float spacing) {
        return SiteLattice.sharedOn(spacing);
    }

    @ModifyExpressionValue(method = "applyAtoll",
            at = @At(value = "FIELD", target = GRID_SIZE, opcode = Opcodes.GETFIELD, ordinal = ORIGIN_X_READ))
    private float exotic_worlds$originX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyExpressionValue(method = "applyAtoll",
            at = @At(value = "FIELD", target = GRID_SIZE, opcode = Opcodes.GETFIELD, ordinal = ORIGIN_Z_READ))
    private float exotic_worlds$originZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }
}
