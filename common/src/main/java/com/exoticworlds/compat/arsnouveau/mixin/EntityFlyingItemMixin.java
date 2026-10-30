package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;
import com.hollingsworth.arsnouveau.common.entity.EntityFlyingItem;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = EntityFlyingItem.class, remap = false)
public abstract class EntityFlyingItemMixin {
    @ModifyVariable(method = "tick()V", at = @At("STORE"), ordinal = 0)
    private Vec3 exotic_worlds$startNearItem(Vec3 start) {
        return this.exotic_worlds$nearItem(start);
    }

    @ModifyVariable(method = "tick()V", at = @At("STORE"), ordinal = 1)
    private Vec3 exotic_worlds$endNearItem(Vec3 end) {
        return this.exotic_worlds$nearItem(end);
    }

    @Unique
    private Vec3 exotic_worlds$nearItem(Vec3 point) {
        Entity item = (Entity) (Object) this;
        return NearestCopy.toward(WorldLoopAttachments.wrappedFoldOfReader(item.level()), item.position(),
                point);
    }
}
