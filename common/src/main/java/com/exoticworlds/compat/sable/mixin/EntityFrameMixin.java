package com.exoticworlds.compat.sable.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.sable.SeamFrame;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

@Mixin(Entity.class)
public abstract class EntityFrameMixin {
    @Shadow
    public abstract Level level();

    @WrapMethod(method = "getOnPos(F)Lnet/minecraft/core/BlockPos;")
    private BlockPos toroidal$frameOnFootQuery(float distance, Operation<BlockPos> original) {
        return SeamFrame.with(this.level(), (Entity) (Object) this, Entity::position, original, distance);
    }
}
