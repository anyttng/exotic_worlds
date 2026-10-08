package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.accessors.ClimateFieldMark;
import com.exoticworlds.shape.climate.ClimateFields;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

@Mixin(DensityFunction.NoiseHolder.class)
public class NoiseHolderMixin {
    @Inject(method = "<init>(Lnet/minecraft/core/Holder;Lnet/minecraft/world/level/levelgen/synth/NormalNoise;)V",
            at = @At("RETURN"))
    private void toroidal$markClimateField(Holder<NormalNoise.NoiseParameters> noiseData, NormalNoise noise,
            CallbackInfo callback) {
        if (noise instanceof ClimateFieldMark mark
                && noiseData.unwrapKey().filter(ClimateFields::isClimate).isPresent()) {
            mark.toroidal$markClimateField();
        }
    }
}
