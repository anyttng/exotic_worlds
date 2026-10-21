package com.exoticworlds.compat.ultimatemapatlases.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.ultimatemapatlases.AtlasCopyLookup;
import com.exoticworlds.compat.ultimatemapatlases.UltimateMapAtlasesInjectionTargets;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.item.context.UseOnContext;
import net.nanaky.ultimate_map_atlases.item.MapAtlasItem;
import net.nanaky.ultimate_map_atlases.map_collection.IMapCollection;
import net.nanaky.ultimate_map_atlases.map_collection.MapKey;
import net.nanaky.ultimate_map_atlases.utils.MapDataHolder;

@Mixin(MapAtlasItem.class)
public class MapAtlasItemMixin {
    @WrapOperation(method = "useOn",
            at = @At(value = "INVOKE", target = UltimateMapAtlasesInjectionTargets.SELECT_KEY))
    private @Nullable MapDataHolder exotic_worlds$bannerMapAtAnyCopy(IMapCollection maps, MapKey key,
            Operation<MapDataHolder> original, @Local(argsOnly = true) UseOnContext context) {
        return AtlasCopyLookup.atAnyCopy(context.getLevel(), maps, key, original);
    }
}
