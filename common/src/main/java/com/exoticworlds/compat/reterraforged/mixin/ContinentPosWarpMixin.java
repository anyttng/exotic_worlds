package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.terraforged.ContinentWarp;
import com.exoticworlds.compat.terraforged.RtfLap;

import net.minecraft.core.Direction;
import raccoonman.reterraforged.world.worldgen.cell.continent.advanced.AdvancedContinentGenerator;
import raccoonman.reterraforged.world.worldgen.cell.continent.uplift.UpliftContinentGenerator;
import raccoonman.reterraforged.world.worldgen.noise.domain.Domain;
import raccoonman.reterraforged.world.worldgen.util.PosUtil;

@Mixin(value = {AdvancedContinentGenerator.class, UpliftContinentGenerator.class}, remap = false)
public abstract class ContinentPosWarpMixin {
    @Shadow(remap = false)
    protected float frequency;

    @WrapOperation(method = "apply", at = @At(value = "INVOKE",
            target = "Lraccoonman/reterraforged/world/worldgen/noise/domain/Domain;getPos(FFI)J"))
    private long exotic_worlds$warpInCells(Domain warp, float x, float z, int seed, Operation<Long> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(warp, x, z, seed);
        }

        float xRatio = ContinentWarp.ratio(frame, Direction.Axis.X, this.frequency);
        float zRatio = ContinentWarp.ratio(frame, Direction.Axis.Z, this.frequency);
        try (RtfLap.Frame.Scope cells = ContinentWarp.enter(frame, xRatio, zRatio)) {
            float u = x / xRatio;
            float v = z / zRatio;
            long warped = original.call(warp, u, v, seed);
            return PosUtil.packf(x + (PosUtil.unpackLeftf(warped) - u) * xRatio,
                    z + (PosUtil.unpackRightf(warped) - v) * zRatio);
        }
    }
}
