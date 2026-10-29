package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.accessors.FoldSource;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.gen.CanonicalRandomFactory;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;

@Mixin(MaterialRuleContext.class)
public class MaterialRuleContextMixin implements FoldSource {
    @Shadow
    @Final
    private RandomState randomState;

    @ModifyReturnValue(method = "getOrCreateRandomFactory", at = @At("RETURN"))
    private PositionalRandomFactory exotic_worlds$seedFromCanonicalPosition(PositionalRandomFactory factory) {
        WorldFold fold = this.exotic_worlds$wrappedFold();
        return fold == null ? factory : new CanonicalRandomFactory(factory, fold);
    }

    @Override
    public @Nullable WorldFold exotic_worlds$wrappedFold() {
        return ((FoldSource) (Object) this.randomState).exotic_worlds$wrappedFold();
    }
}
