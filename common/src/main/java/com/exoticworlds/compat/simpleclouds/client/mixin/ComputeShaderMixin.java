package com.exoticworlds.compat.simpleclouds.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.simpleclouds.CloudRegionShaderSource;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import dev.nonamecrackers2.simpleclouds.client.shader.compute.ComputeShader;

@Mixin(value = ComputeShader.class, remap = false)
public abstract class ComputeShaderMixin {
    @ModifyExpressionValue(method = "compileShader", at = @At(value = "INVOKE",
            target = "Lorg/apache/commons/io/IOUtils;toString(Ljava/io/InputStream;Ljava/nio/charset/Charset;)Ljava/lang/String;"))
    private static String toroidal$seatRegionField(String source, @Local(argsOnly = true, ordinal = 0) String loc) {
        return source != null && CloudRegionShaderSource.SHADER_ID.equals(loc)
                ? CloudRegionShaderSource.rewrite(source)
                : source;
    }
}
