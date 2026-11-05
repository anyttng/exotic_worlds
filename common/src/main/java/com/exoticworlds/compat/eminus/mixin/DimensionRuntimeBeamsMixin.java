package com.exoticworlds.compat.eminus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.eminus.session.DimensionRuntime;
import com.exoticworlds.compat.eminus.CellFoldHolder;

@Mixin(DimensionRuntime.class)
public class DimensionRuntimeBeamsMixin {
    @Inject(method = "openBeams", at = @At("RETURN"))
    private void exotic_worlds$foldTheBeams(CallbackInfoReturnable<Object> cir) {
        CellFoldHolder.setFold(cir.getReturnValue(), CellFoldHolder.foldOf(this));
    }
}
