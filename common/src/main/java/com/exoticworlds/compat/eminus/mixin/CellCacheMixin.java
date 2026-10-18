package com.exoticworlds.compat.eminus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.eminus.cell.cache.CellCache;
import com.exoticworlds.compat.eminus.CellFold;
import com.exoticworlds.compat.eminus.CellFoldHolder;

@Mixin(CellCache.class)
public class CellCacheMixin implements CellFoldHolder {
    @Unique
    private volatile CellFold exotic_worlds$fold = CellFold.NONE;

    @Override
    public CellFold exotic_worlds$fold() {
        return exotic_worlds$fold;
    }

    @Override
    public void exotic_worlds$setFold(CellFold fold) {
        exotic_worlds$fold = fold;
    }

    @ModifyVariable(method = "open", at = @At("HEAD"), argsOnly = true)
    private long exotic_worlds$openStoredKey(long key) {
        return exotic_worlds$fold.foldKey(key);
    }
}
