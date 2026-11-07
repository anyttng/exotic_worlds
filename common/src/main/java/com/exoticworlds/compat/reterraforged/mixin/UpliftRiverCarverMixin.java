package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.exoticworlds.compat.reterraforged.ReTerraForgedRiverReach;

import raccoonman.reterraforged.world.worldgen.cell.rivermap.river.UpliftRiverCarver;

// The carver sizes every zone, the valley included, against this distance, so no generator scales the valley as well.
@Mixin(value = UpliftRiverCarver.class, remap = false)
public abstract class UpliftRiverCarverMixin {
    @Unique
    private static final String CONSTRUCTOR = "<init>(Lraccoonman/reterraforged/world/worldgen/cell/rivermap/river/"
            + "River;Lraccoonman/reterraforged/world/worldgen/cell/rivermap/river/RiverWarp;"
            + "Lraccoonman/reterraforged/world/worldgen/cell/rivermap/river/RiverConfig;"
            + "Lraccoonman/reterraforged/world/worldgen/cell/rivermap/river/RiverCarverSettings;"
            + "Lraccoonman/reterraforged/world/worldgen/cell/heightmap/Levels;"
            + "Lraccoonman/reterraforged/world/worldgen/cell/rivermap/lake/LakeConfig;Z)V";

    @Unique
    private float exotic_worlds$lapScale;

    @Inject(method = CONSTRUCTOR, at = @At("RETURN"))
    private void exotic_worlds$keepTheLapScale(CallbackInfo ci) {
        this.exotic_worlds$lapScale = ReTerraForgedRiverReach.INSTANCE.boundScale();
    }

    @ModifyReturnValue(method = "getDistance2", at = @At("RETURN"))
    private float exotic_worlds$distanceAtFullSize(float distanceSquared) {
        return distanceSquared / (this.exotic_worlds$lapScale * this.exotic_worlds$lapScale);
    }
}
