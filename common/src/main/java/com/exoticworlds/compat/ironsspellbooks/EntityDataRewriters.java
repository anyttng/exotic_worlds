package com.exoticworlds.compat.ironsspellbooks;

import org.joml.Vector3f;

import com.exoticworlds.compat.ironsspellbooks.mixin.DeadKingSoulEntityAccessor;
import com.exoticworlds.engine.fold.NearestCopy;
import com.exoticworlds.engine.net.PacketTranslator;

import io.redspace.ironsspellbooks.entity.mobs.dead_king_boss.DeadKingSoulEntity;

import net.minecraft.world.phys.Vec3;

final class EntityDataRewriters {
    static void register() {
        PacketTranslator.registerEntityDataRewriter(DeadKingSoulEntity.class,
                DeadKingSoulEntityAccessor::exotic_worlds$getDataRespawnPos,
                (respawn, context, soul) -> {
                    Vec3 raw = new Vec3(respawn);
                    Vec3 seated = NearestCopy.toward(context.fold(), soul, raw);
                    return seated.equals(raw) ? respawn : seated.toVector3f();
                });
    }

    private EntityDataRewriters() {
    }
}
