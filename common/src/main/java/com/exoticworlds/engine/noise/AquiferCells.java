package com.exoticworlds.engine.noise;

import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.densityfunction.op.CacheFunction;

public record AquiferCells(TilingCellGrid levelGrid, DensitySampler level, TilingCellGrid typeGrid,
        DensitySampler type) {
    private static final DfRewriteRule UNCACHED = new DfRewriteRule() {
        @Override
        public DensityFunction rewrite(DensityFunction function) {
            DensityFunction inlined = DfRewriteRule.INLINE_REFERENCE.rewrite(function);
            return inlined instanceof CacheFunction cache ? this.rewrite(cache.input()) : inlined.rewriteChildren(this);
        }
    };

    public static AquiferCells compile(FoldedCompileContext context, Aquifer.Config config) {
        TilingCellGrid levelGrid = TilingCellGrid.of(context.fold(), NoiseConstants.AQUIFER_FLUID_LEVEL_CELL_WIDTH);
        TilingCellGrid typeGrid = TilingCellGrid.of(context.fold(), NoiseConstants.AQUIFER_FLUID_TYPE_CELL_WIDTH);
        return new AquiferCells(
                levelGrid, compileOnGrid(context, levelGrid, config.fluidLevelSpreadNoise()),
                typeGrid, compileOnGrid(context, typeGrid, config.lavaNoise()));
    }

    public float level(int blockX, int cellY, int blockZ) {
        return this.level.sampleValue(SamplerContext.EMPTY_UNCACHED,
                this.levelGrid.cellOriginX(blockX), cellY, this.levelGrid.cellOriginZ(blockZ));
    }

    public float type(int blockX, int cellY, int blockZ) {
        return this.type.sampleValue(SamplerContext.EMPTY_UNCACHED,
                this.typeGrid.cellOriginX(blockX), cellY, this.typeGrid.cellOriginZ(blockZ));
    }

    private static DensitySampler compileOnGrid(FoldedCompileContext context, TilingCellGrid grid,
            DensityFunction function) {
        return UNCACHED.rewrite(function).compileSampler(context.withDivisors(grid.xCellWidth(), grid.zCellWidth()));
    }
}
