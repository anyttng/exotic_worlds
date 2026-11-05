package com.exoticworlds.compat.sable.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.sable.SeamFrame;

import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

@Mixin(Explosion.class)
public abstract class ExplosionFrameMixin {
    @Shadow
    @Final
    private Level level;

    @WrapMethod(method = "explode")
    private void exotic_worlds$frameOnExplosion(Operation<Void> original) {
        Object entered = SeamFrame.enter(this.level, (Explosion) (Object) this, Explosion::center);
        try {
            original.call();
        } finally {
            SeamFrame.exit(entered);
        }
    }
}
