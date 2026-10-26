package com.exoticworlds.compat.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.terraforged.RtfLap;

import com.pandaismyname1.ultraterraforged.world.worldgen.noise.domain.Domain;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.RiverRoutes;

@Mixin(value = RiverRoutes.class, remap = false)
public abstract class RiverRoutesMixin {
    @WrapMethod(method = "unwarp")
    private static long exotic_worlds$unwarpOpen(float x, float z, Domain warp, Operation<Long> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(x, z, warp);
        }

        try (RtfLap.Frame.Scope open = frame.open()) {
            return original.call(x, z, warp);
        }
    }
}
