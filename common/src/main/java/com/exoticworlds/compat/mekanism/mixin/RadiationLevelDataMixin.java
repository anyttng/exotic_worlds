package com.exoticworlds.compat.mekanism.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.mekanism.RadiationIndexFrame;
import com.exoticworlds.compat.mekanism.RadiationLevelFrame;
import com.exoticworlds.core.WorldFold;

import mekanism.common.lib.collection.IndexedCuboidMap;
import mekanism.common.lib.radiation.RadiationLevelData;
import mekanism.common.lib.radiation.RadiationSource;

import net.minecraft.core.BlockPos;

@Mixin(value = RadiationLevelData.class, remap = false)
public abstract class RadiationLevelDataMixin implements RadiationLevelFrame {
    @Shadow
    @Final
    private IndexedCuboidMap<RadiationSource> sources;

    @Shadow
    public abstract void radiate(BlockPos pos, double magnitude);

    @Override
    public void toroidal$bind(WorldFold fold) {
        RadiationIndexFrame index = (RadiationIndexFrame) this.sources;
        if (index.toroidal$fold() == fold) {
            return;
        }

        index.toroidal$bind(fold);
        List<RadiationSource> unseated = this.sources.values().stream()
                .filter(source -> !fold.fold(source.getPosition()).equals(source.getPosition()))
                .toList();
        for (RadiationSource source : unseated) {
            this.sources.remove(source);
            this.radiate(source.getPosition(), source.getMagnitude());
        }
    }

    @ModifyVariable(method = {"radiate", "removeRadiationSource"}, at = @At("HEAD"), argsOnly = true, require = 2)
    private BlockPos toroidal$foldSourcePosition(BlockPos pos) {
        return ((RadiationIndexFrame) this.sources).toroidal$fold().fold(pos);
    }

    @WrapOperation(method = "getRadiationLevelAndMaxMagnitude",
            at = @At(value = "INVOKE", target = "Lmekanism/common/lib/radiation/RadiationUtil;computeExposure("
                    + "Lmekanism/common/lib/radiation/RadiationSource;Lnet/minecraft/core/BlockPos;)D"))
    private double toroidal$exposureFromNearestCopy(RadiationSource source, BlockPos checkPos,
            Operation<Double> original) {
        WorldFold fold = ((RadiationIndexFrame) this.sources).toroidal$fold();
        return original.call(source, fold.nearestCopy(source.getPosition(), checkPos));
    }
}
