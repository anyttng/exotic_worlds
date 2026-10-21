package com.exoticworlds.compat.ultimatemapatlases.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.ultimatemapatlases.AtlasCopyLookup;
import com.exoticworlds.compat.ultimatemapatlases.UltimateMapAtlasesInjectionTargets;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.Level;
import net.nanaky.ultimate_map_atlases.map_collection.IMapCollection;
import net.nanaky.ultimate_map_atlases.map_collection.MapKey;
import net.nanaky.ultimate_map_atlases.recipe.MapAtlasesAddRecipe;
import net.nanaky.ultimate_map_atlases.utils.MapDataHolder;

@Mixin(MapAtlasesAddRecipe.class)
public class MapAtlasesAddRecipeMixin {
    @WrapOperation(
            method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
            at = @At(value = "INVOKE", target = UltimateMapAtlasesInjectionTargets.SELECT_KEY))
    private @Nullable MapDataHolder exotic_worlds$addedMapAtAnyCopy(IMapCollection maps, MapKey key,
            Operation<MapDataHolder> original, @Local(argsOnly = true) Level level) {
        return AtlasCopyLookup.atAnyCopy(level, maps, key, original);
    }
}
