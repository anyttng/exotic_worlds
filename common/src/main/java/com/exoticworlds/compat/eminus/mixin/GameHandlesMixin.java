package com.exoticworlds.compat.eminus.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.eminus.gpu.Location;
import com.exoticworlds.compat.eminus.LapClip;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(targets = "com.eminus.client.gpu.opengl.GameHandles")
public class GameHandlesMixin {
    @ModifyReturnValue(method = "resource", at = @At("RETURN"))
    private static Optional<String> exotic_worlds$clipToTheLap(Optional<String> source,
            @Local(argsOnly = true) Location file) {
        return source.map(text -> LapClip.rewrite(file.namespace(), LapClip.fragmentShader(file.path()), text));
    }
}
