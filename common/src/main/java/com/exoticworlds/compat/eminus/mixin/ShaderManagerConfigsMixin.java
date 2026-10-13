package com.exoticworlds.compat.eminus.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.eminus.LapClip;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.ShaderType;

import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;

@Mixin(ShaderManager.Configs.class)
public class ShaderManagerConfigsMixin {
    @ModifyReturnValue(method = "getShader", at = @At("RETURN"))
    private @Nullable String toroidal$clipEminusToTheLap(@Nullable String source,
            @Local(argsOnly = true) Identifier id, @Local(argsOnly = true) ShaderType type) {
        return type == ShaderType.FRAGMENT ? LapClip.rewrite(id.getNamespace(), id.getPath(), source) : source;
    }
}
