package com.exoticworlds.engine.noise;

import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.densityfunction.op.CacheFunction;

public record AquiferCells(AquiferCellGrid levelGrid, DensitySampler level, AquiferCellGrid typeGrid,
        DensitySampler type) {
    private static final DfRewriteRule UNCACHED = new DfRewriteRule() {
        @Override
        public DensityFunction rewrite(DensityFunction function) {
            DensityFunction inlined = DfRewriteRule.INLINE_REFERENCE.rewrite(function);
            return inlined instanceof CacheFunction cache ? this.rewrite(cache.input()) : inlined.rewriteChildren(this);
        }
    };

    public static AquiferCells compile(FoldedCompileContext context, Aquifer.Config config) {
        AquiferCellGrid levelGrid = AquiferCellGrid.of(context.fold(),
                NoiseConstants.AQUIFER_FLUID_LEVEL_CELL_WIDTH_BLOCKS);
        AquiferCellGrid typeGrid = AquiferCellGrid.of(context.fold(), NoiseConstants.AQUIFER_FLUID_TYPE_CELL_WIDTH_BLOCKS);
        return new AquiferCells(
                levelGrid, compileOnGrid(context, levelGrid, config.fluidLevelSpreadNoise()),
                typeGrid, compileOnGrid(context, typeGrid, config.lavaNoise()));
    }

    public float level(int blockX, int cellY, int blockZ) {
        return this.level.sampleValue(SamplerContext.EMPTY_UNCACHED,
                this.levelGrid.cellOriginX(blockX, blockZ), cellY, this.levelGrid.cellOriginZ(blockX, blockZ));
    }

    public float type(int blockX, int cellY, int blockZ) {
        return this.type.sampleValue(SamplerContext.EMPTY_UNCACHED,
                this.typeGrid.cellOriginX(blockX, blockZ), cellY, this.typeGrid.cellOriginZ(blockX, blockZ));
    }

    private static DensitySampler compileOnGrid(FoldedCompileContext context, AquiferCellGrid grid,
            DensityFunction function) {
        try (GenerationFoldContext.Context.DivisorScope _ = grid.divide(GenerationFoldContext.getContext())) {
            return UNCACHED.rewrite(function).compileSampler(context.vanilla());
        }
    }
}
