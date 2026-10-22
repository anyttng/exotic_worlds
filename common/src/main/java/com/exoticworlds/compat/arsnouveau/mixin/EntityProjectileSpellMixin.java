package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldLoopAttachments;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

@Mixin(value = EntityProjectileSpell.class, remap = false)
public abstract class EntityProjectileSpellMixin {
    private static final String ON_HIT = "onHit(Lnet/minecraft/world/phys/HitResult;)V";

    @ModifyArg(method = ON_HIT, at = @At(value = "INVOKE", target = InjectionTargets.SET_CONTAINS))
    private Object exotic_worlds$hitListLookupThroughSeam(Object hitBlock) {
        return exotic_worlds$canonical(hitBlock);
    }

    @ModifyArg(method = ON_HIT, at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"))
    private Object exotic_worlds$hitListEntryThroughSeam(Object hitBlock) {
        return exotic_worlds$canonical(hitBlock);
    }

    @Unique
    private BlockPos exotic_worlds$canonical(Object hitBlock) {
        return WorldLoopAttachments.transformerOf(((Entity) (Object) this).level()).fold((BlockPos) hitBlock);
    }
}
