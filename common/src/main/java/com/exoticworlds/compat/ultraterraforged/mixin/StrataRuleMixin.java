package com.exoticworlds.compat.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.terraforged.RtfEntry;
import com.exoticworlds.compat.terraforged.RtfLap;
import com.exoticworlds.core.WorldFold;

@Mixin(targets = "com.pandaismyname1.ultraterraforged.world.worldgen.surface.rule.StrataRule$Rule", remap = false)
public abstract class StrataRuleMixin {
    @WrapMethod(method = "updateColumn")
    private void exotic_worlds$bindLap(int x, int z, Operation<Void> original) {
        WorldFold fold = RtfEntry.generationFold();
        if (fold == null) {
            original.call(x, z);
            return;
        }

        try (RtfLap.Frame.Scope bound = RtfLap.frame().bind(fold)) {
            original.call(RtfEntry.foldX(fold, x, z), RtfEntry.foldZ(fold, z));
        }
    }
}
