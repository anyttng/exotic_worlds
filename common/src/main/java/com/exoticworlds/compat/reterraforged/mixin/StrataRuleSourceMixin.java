package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.reterraforged.RtfEntry;
import com.exoticworlds.compat.reterraforged.RtfLap;
import com.exoticworlds.core.WorldFold;

import net.minecraft.core.Direction;

@Mixin(targets = "raccoonman.reterraforged.world.worldgen.surface.rule.StrataRule$Source", remap = false)
public abstract class StrataRuleSourceMixin {
    @WrapMethod(method = "initBuffer")
    private void toroidal$bindLap(int x, int z, Operation<Void> original) {
        WorldFold fold = RtfEntry.generationFold();
        if (fold == null) {
            original.call(x, z);
            return;
        }

        try (RtfLap.Frame.Scope lap = RtfLap.frame().bind(fold)) {
            original.call(RtfEntry.foldBlock(fold, Direction.Axis.X, x), RtfEntry.foldBlock(fold, Direction.Axis.Z, z));
        }
    }
}
