package com.exoticworlds.compat.mapatlases.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.map.ClientFolds;
import com.exoticworlds.compat.mapatlases.MapAtlasesInjectionTargets;
import com.exoticworlds.compat.mapatlases.MapGridKeyCopies;
import com.exoticworlds.core.fold.WorldFold;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import pepjebs.mapatlases.client.screen.AtlasOverviewScreen;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.map_collection.MapGridKey;
import pepjebs.mapatlases.utils.AtlasMap;
import pepjebs.mapatlases.utils.Slice;

@Mixin(AtlasOverviewScreen.class)
public class AtlasOverviewScreenMixin {
    @Shadow
    private Slice selectedSlice;

    @WrapOperation(
            method = "findMapContaining",
            at = @At(value = "INVOKE", target = MapAtlasesInjectionTargets.GET_MAP_AT_KEY))
    private @Nullable AtlasMap exotic_worlds$mapAtAnyCopy(MapCollection maps, MapGridKey key,
            Operation<AtlasMap> original, @Local(argsOnly = true, ordinal = 0) int x,
            @Local(argsOnly = true, ordinal = 1) int z) {
        @Nullable WorldFold fold = ClientFolds.of(this.selectedSlice.dimension());
        for (MapGridKey copy : MapGridKeyCopies.copies(fold, key)) {
            @Nullable AtlasMap map = original.call(maps, copy);
            if (map != null) {
                return map;
            }
        }

        return MapGridKeyCopies.getShiftedUnder(fold, maps, key, x, z);
    }
}
