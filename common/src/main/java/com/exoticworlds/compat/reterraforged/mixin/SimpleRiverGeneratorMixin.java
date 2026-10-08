package com.exoticworlds.compat.reterraforged.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.reterraforged.RiverReach;
import com.exoticworlds.compat.reterraforged.RtfLap;

import raccoonman.reterraforged.world.worldgen.cell.continent.SimpleContinent;
import raccoonman.reterraforged.world.worldgen.cell.continent.simple.SimpleRiverGenerator;
import raccoonman.reterraforged.world.worldgen.cell.rivermap.gen.GenWarp;
import raccoonman.reterraforged.world.worldgen.cell.rivermap.river.Network;

@Mixin(value = SimpleRiverGenerator.class, remap = false)
public abstract class SimpleRiverGeneratorMixin {
    private static final String DISTANCE_TO_OCEAN =
            "Lraccoonman/reterraforged/world/worldgen/cell/continent/SimpleContinent;getDistanceToOcean(IIFF)F";

    // A world too narrow for a root to run past its own start carries no rivers.
    @WrapMethod(method = "generateRoots")
    private List<Network.Builder> toroidal$rootsThatFit(int x, int z, Random random, GenWarp warp,
            Operation<List<Network.Builder>> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null || RiverReach.fits(frame)) {
            return original.call(x, z, random, warp);
        }

        return new ArrayList<>();
    }

    @WrapOperation(method = "generateRoots", at = @At(value = "INVOKE", target = DISTANCE_TO_OCEAN))
    private float toroidal$fitInsideHalfLap(SimpleContinent continent, int x, int z, float dx, float dz,
            Operation<Float> original) {
        float length = original.call(continent, x, z, dx, dz);
        RtfLap.Frame frame = RtfLap.boundFrame();
        return frame == null ? length : RiverReach.clamp(frame, length, dx, dz);
    }
}
