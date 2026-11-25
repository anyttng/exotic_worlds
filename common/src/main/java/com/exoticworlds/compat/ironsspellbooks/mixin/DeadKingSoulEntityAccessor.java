package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import io.redspace.ironsspellbooks.entity.mobs.dead_king_boss.DeadKingSoulEntity;

import net.minecraft.network.syncher.EntityDataAccessor;

@Mixin(value = DeadKingSoulEntity.class, remap = false)
public interface DeadKingSoulEntityAccessor {
    @Accessor("DATA_RESPAWN_POS")
    static EntityDataAccessor<Vector3f> exotic_worlds$getDataRespawnPos() {
        throw new AssertionError();
    }
}
