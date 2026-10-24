package com.exoticworlds.compat.ultraterraforged.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.ultraterraforged.RiverReach;
import com.exoticworlds.compat.terraforged.RtfLap;

import com.pandaismyname1.ultraterraforged.world.worldgen.continent.SimpleContinent;
import com.pandaismyname1.ultraterraforged.world.worldgen.continent.simple.SimpleRiverGenerator;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.gen.GenWarp;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.river.Network;

@Mixin(value = SimpleRiverGenerator.class, remap = false)
public abstract class SimpleRiverGeneratorMixin {
    @Unique
    private static final String DISTANCE_TO_OCEAN =
            "Lcom/pandaismyname1/ultraterraforged/world/worldgen/continent/SimpleContinent;getDistanceToOcean(IIFF)F";

    @Unique
    private static final String COUNT =
            "Lcom/pandaismyname1/ultraterraforged/world/worldgen/continent/simple/SimpleRiverGenerator;count:I";

    @ModifyExpressionValue(method = "generateRoots",
            at = @At(value = "FIELD", target = COUNT, opcode = Opcodes.GETFIELD))
    private int exotic_worlds$rootsForTheCell(int count) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return count;
        }

        return RiverReach.rootCount(frame, count,
                ((BaseRiverGeneratorAccessor) (Object) this).exotic_worlds$continentScale());
    }

    // A world too narrow for a root to run past its own start carries no rivers.
    @WrapMethod(method = "generateRoots")
    private List<Network.Builder> exotic_worlds$rootsThatFit(int x, int z, Random random, GenWarp warp,
            Operation<List<Network.Builder>> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null || RiverReach.fits(frame)) {
            return original.call(x, z, random, warp);
        }

        return new ArrayList<>();
    }

    @WrapOperation(method = "generateRoots", at = @At(value = "INVOKE", target = DISTANCE_TO_OCEAN))
    private float exotic_worlds$fitInsideHalfLap(SimpleContinent continent, int x, int z, float dx, float dz,
            Operation<Float> original) {
        float length = original.call(continent, x, z, dx, dz);
        RtfLap.Frame frame = RtfLap.boundFrame();
        return frame == null ? length : RiverReach.clamp(frame, length, dx, dz);
    }
}
