package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.gen.CanonicalRandomFactory;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;

@Mixin(MaterialRuleContext.class)
public class MaterialRuleContextMixin {
    @Shadow
    @Final
    private RandomState randomState;

    @ModifyReturnValue(method = "getOrCreateRandomFactory", at = @At("RETURN"))
    private PositionalRandomFactory toroidal$seedFromCanonicalPosition(PositionalRandomFactory factory) {
        WorldFold fold = ((TransformerSource) (Object) this.randomState).toroidal$wrappedTransformer();
        return fold == null ? factory : new CanonicalRandomFactory(factory, fold);
    }
}
