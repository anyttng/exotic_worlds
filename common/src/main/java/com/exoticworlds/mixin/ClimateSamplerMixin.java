package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.BinderOrder;
import com.exoticworlds.accessors.FoldedClimateSampler;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.engine.fold.FoldedQuart;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.biome.Climate;

@Mixin(Climate.Sampler.class)
public class ClimateSamplerMixin implements FoldedClimateSampler {
    @Unique
    private WorldFold exotic_worlds$fold = WorldFolds.NOOP;

    @WrapMethod(method = "sample", order = BinderOrder.FOLD)
    private Climate.TargetPoint exotic_worlds$sampleTheCanonicalCopy(int quartX, int quartY, int quartZ,
            Operation<Climate.TargetPoint> original) {
        long folded = FoldedQuart.fold(this.exotic_worlds$fold, quartX, quartY, quartZ);
        return original.call(FoldedQuart.x(folded), quartY, FoldedQuart.z(folded));
    }

    @Override
    public WorldFold exotic_worlds$fold() {
        return this.exotic_worlds$fold;
    }

    @Override
    public void exotic_worlds$fold(WorldFold fold) {
        this.exotic_worlds$fold = fold;
    }
}
