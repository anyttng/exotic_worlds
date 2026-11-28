package com.exoticworlds.compat.mapatlases.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.mapatlases.MapAtlasesInjectionTargets;
import com.exoticworlds.compat.mapatlases.MapGridKeyCopies;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.map_collection.MapGridKey;
import pepjebs.mapatlases.utils.AtlasMap;

@Mixin(MapAtlasItem.class)
public class MapAtlasItemMixin {
    @WrapOperation(method = "useOn", at = @At(value = "INVOKE", target = MapAtlasesInjectionTargets.GET_MAP_AT_KEY))
    private @Nullable AtlasMap exotic_worlds$bannerMapUnderPlayer(MapCollection maps, MapGridKey key,
            Operation<AtlasMap> original, @Local(argsOnly = true) UseOnContext context) {
        for (MapGridKey copy : MapGridKeyCopies.copies(context.getLevel(), key)) {
            @Nullable AtlasMap map = original.call(maps, copy);
            if (map != null) {
                return map;
            }
        }

        Player player = context.getPlayer();
        return player == null ? null
                : MapGridKeyCopies.getShiftedUnder(context.getLevel(), maps, key, player.getX(), player.getZ());
    }
}
