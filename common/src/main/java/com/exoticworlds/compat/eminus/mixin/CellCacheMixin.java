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
    private volatile CellFold toroidal$fold = CellFold.NONE;

    @Override
    public CellFold toroidal$fold() {
        return toroidal$fold;
    }

    @Override
    public void toroidal$setFold(CellFold fold) {
        toroidal$fold = fold;
    }

    @ModifyVariable(method = "open", at = @At("HEAD"), argsOnly = true)
    private long toroidal$openStoredKey(long key) {
        return toroidal$fold.foldKey(key);
    }
}
