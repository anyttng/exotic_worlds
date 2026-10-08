package com.exoticworlds.compat.lithium.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

@Pseudo
@Mixin(targets = {
        "net.caffeinemc.mods.lithium.fabric.FabricEntityAccess",
        "net.caffeinemc.mods.lithium.neoforge.NeoForgeEntityAccess"
})
public class EnderDragonPartsMixin {
    @WrapOperation(method = "addEnderDragonParts", at = @At(value = "INVOKE", target = InjectionTargets.AABB_INTERSECTS))
    private boolean toroidal$partBoxTowardQuery(AABB query, AABB part, Operation<Boolean> original,
            @Local(argsOnly = true) Level level) {
        return original.call(query,
                FoldedBoxQuery.toward(WorldLoopAttachments.wrappedTransformerOf(level), query.getCenter(), part));
    }
}
