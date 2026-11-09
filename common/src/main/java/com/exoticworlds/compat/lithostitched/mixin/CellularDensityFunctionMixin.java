package com.exoticworlds.compat.lithostitched.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.lithostitched.CellLap;
import com.exoticworlds.compat.lithostitched.ClosedGrid;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.GenerationFoldContext.Context;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import dev.worldgen.lithostitched.impl.worldgen.densityfunction.CellularDensityFunction;
import net.minecraft.world.level.levelgen.WorldgenRandom;

@Mixin(CellularDensityFunction.class)
public class CellularDensityFunctionMixin {
    private static final String EXPRESSION = "MIXINEXTRAS:EXPRESSION";

    private static final int SEED_USE = 0;

    private static final String SAMPLE_2D = "sample2D(II)D";

    private static final String SAMPLE_3D = "sample3D(III)D";

    private static final String GRID_DIMENSIONS = "Ldev/worldgen/lithostitched/impl/worldgen/densityfunction/"
            + "CellularDensityFunction;gridDimensions:Ldev/worldgen/lithostitched/impl/worldgen/densityfunction/"
            + "CellularDensityFunction$GridDimensions;";

    @Unique
    private volatile @Nullable ClosedGrid exotic_worlds$closedGrid;

    @ModifyExpressionValue(method = {SAMPLE_2D, SAMPLE_3D}, at = @At(value = "FIELD", target = GRID_DIMENSIONS))
    private CellularDensityFunction.GridDimensions exotic_worlds$closedDimensions(
            CellularDensityFunction.GridDimensions dimensions) {
        ClosedGrid grid = this.exotic_worlds$closedGrid(dimensions);
        return grid == null ? dimensions : grid.dimensions();
    }

    @WrapOperation(method = SAMPLE_2D, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/WorldgenRandom;setLargeFeatureWithSalt(JIII)V"))
    private void exotic_worlds$closedCellSeed(WorldgenRandom random, long seed, int cellX, int cellZ, int salt,
            Operation<Void> original) {
        CellLap lap = this.exotic_worlds$cellLap();
        if (lap == null) {
            original.call(random, seed, cellX, cellZ, salt);
            return;
        }

        original.call(random, seed, lap.foldX(cellX, cellZ), lap.foldZ(cellZ), salt);
    }

    @Definition(id = "spacedGridX", local = @Local(type = int.class, name = "spacedGridX"))
    @Definition(id = "xo", local = @Local(type = int.class, name = "xo"))
    @Expression("spacedGridX + xo")
    @ModifyExpressionValue(method = SAMPLE_3D, at = @At(value = EXPRESSION, ordinal = SEED_USE))
    private int exotic_worlds$closedSeedX(int cellX, @Local(name = "spacedGridZ") int spacedGridZ,
            @Local(name = "zo") int zo) {
        CellLap lap = this.exotic_worlds$cellLap();
        return lap == null ? cellX : lap.foldX(cellX, spacedGridZ + zo);
    }

    @Definition(id = "spacedGridZ", local = @Local(type = int.class, name = "spacedGridZ"))
    @Definition(id = "zo", local = @Local(type = int.class, name = "zo"))
    @Expression("spacedGridZ + zo")
    @ModifyExpressionValue(method = SAMPLE_3D, at = @At(value = EXPRESSION, ordinal = SEED_USE))
    private int exotic_worlds$closedSeedZ(int cellZ) {
        CellLap lap = this.exotic_worlds$cellLap();
        return lap == null ? cellZ : lap.foldZ(cellZ);
    }

    @Unique
    private @Nullable CellLap exotic_worlds$cellLap() {
        ClosedGrid grid = this.exotic_worlds$closedGrid(((CellularDensityFunction) (Object) this).gridDimensions());
        return grid == null ? null : grid.lap();
    }

    @Unique
    private @Nullable ClosedGrid exotic_worlds$closedGrid(CellularDensityFunction.GridDimensions dimensions) {
        Context generation = GenerationFoldContext.wrappedContext();
        if (generation == null) {
            return null;
        }

        WorldFold fold = generation.fold();
        ClosedGrid grid = this.exotic_worlds$closedGrid;
        if (grid == null || grid.fold() != fold) {
            grid = ClosedGrid.of(fold, dimensions);
            this.exotic_worlds$closedGrid = grid;
        }

        return grid;
    }
}
