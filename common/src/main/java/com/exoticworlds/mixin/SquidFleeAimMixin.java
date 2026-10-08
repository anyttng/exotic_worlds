package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.engine.seam.SeamAim;

import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "net.minecraft.world.entity.animal.Squid$SquidFleeGoal")
public class SquidFleeAimMixin {
    @Shadow(aliases = {"this$0", "field_6914"})
    @Final
    private Squid squid;

    @ModifyVariable(method = "tick", at = @At(value = "STORE", ordinal = 0))
    private Vec3 toroidal$fleeDeltaThroughSeam(Vec3 delta) {
        return SeamAim.foldDelta(this.squid, delta);
    }
}
