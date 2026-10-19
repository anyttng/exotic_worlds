package com.exoticworlds.compat.cobblemon;

import org.jspecify.annotations.Nullable;

import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.seam.SeamSteering;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class BattleSeat {
    public static @Nullable Vec3 seated(ActiveBattlePokemon pokemon, @Nullable Vec3 initialPos) {
        LivingEntity anchor = anchorOf(pokemon);
        return initialPos == null || anchor == null ? initialPos : SeamSteering.nearestCopy(anchor, initialPos);
    }

    public static @Nullable Vec3 canonical(ActiveBattlePokemon pokemon, @Nullable Vec3 sendOut) {
        LivingEntity anchor = anchorOf(pokemon);
        return sendOut == null || anchor == null
                ? sendOut
                : WorldLoopAttachments.transformerOf(anchor.level()).fold(sendOut);
    }

    private static @Nullable LivingEntity anchorOf(ActiveBattlePokemon pokemon) {
        LivingEntity own = entityOf(pokemon.getActor());
        if (own != null) {
            return own;
        }

        for (BattleActor actor : pokemon.getBattle().getActors()) {
            LivingEntity entity = entityOf(actor);
            if (entity != null) {
                return entity;
            }
        }

        return null;
    }

    private static @Nullable LivingEntity entityOf(BattleActor actor) {
        return actor instanceof EntityBackedBattleActor<?> backed ? backed.getEntity() : null;
    }

    private BattleSeat() {
    }
}
