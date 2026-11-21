package com.exoticworlds.engine.seam;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.NavigationShifter;
import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.FoldOrientation;
import com.exoticworlds.core.SeamTransform;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.mixin.EntityAccessor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.phys.Vec3;

public final class SeamSnap {
    public static boolean intoBounds(Entity entity) {
        DeckTransformation lap = WorldLoopAttachments.foldOf(entity.level()).foldTransformation(entity.position());
        if (lap.isIdentity()) {
            return false;
        }

        withPassengers(entity, lap);
        return true;
    }

    // mc/1.21: Create's carriage moves its riders by the carriage's own lap.
    public static void withPassengers(Entity entity, DeckTransformation lap) {
        moveBody(entity, lap, lap.apply(entity.position()));
        moveHistory(entity, lap);
        turnHeading(entity, lap.orientation());
        if (entity instanceof Mob mob) {
            carryNavigation(mob, lap);
        }

        if (entity instanceof ServerPlayer rider) {
            rider.level().getChunkSource().move(rider);
        }

        for (Entity passenger : entity.getPassengers()) {
            withPassengers(passenger, lap);
        }
    }

    private static void moveBody(Entity entity, DeckTransformation lap, Vec3 to) {
        if (entity instanceof BlockAttachedEntity attached) {
            attached.setPos(Vec3.atCenterOf(lap.apply(attached.getPos())));
        } else {
            entity.setPos(to.x, to.y, to.z);
        }
    }

    private static void moveHistory(Entity entity, DeckTransformation lap) {
        SeamTransform applied = lap.blockTransform();
        double previousX = entity.xo;
        double previousZ = entity.zo;
        entity.xo = applied.applyX(previousX, previousZ);
        entity.zo = applied.applyZ(previousX, previousZ);
        double oldX = entity.xOld;
        double oldZ = entity.zOld;
        entity.xOld = applied.applyX(oldX, oldZ);
        entity.zOld = applied.applyZ(oldX, oldZ);
        EntityAccessor stored = (EntityAccessor) entity;
        @Nullable Vec3 known = stored.exotic_worlds$lastKnownPosition();
        if (known != null) {
            stored.exotic_worlds$setLastKnownPosition(lap.apply(known));
        }
    }

    private static void turnHeading(Entity entity, FoldOrientation orientation) {
        if (orientation.isIdentity()) {
            return;
        }

        entity.setDeltaMovement(orientation.applyToDelta(entity.getDeltaMovement()));
        entity.setYRot(orientation.applyToYaw(entity.getYRot()));
        entity.yRotO = orientation.applyToYaw(entity.yRotO);
        if (entity instanceof LivingEntity living) {
            living.yHeadRot = orientation.applyToYaw(living.yHeadRot);
            living.yHeadRotO = orientation.applyToYaw(living.yHeadRotO);
            living.yBodyRot = orientation.applyToYaw(living.yBodyRot);
            living.yBodyRotO = orientation.applyToYaw(living.yBodyRotO);
        }
    }

    private static void carryNavigation(Mob mob, DeckTransformation lap) {
        ((NavigationShifter) mob.getNavigation()).exotic_worlds$carry(lap);
        ((NavigationShifter) mob.getMoveControl()).exotic_worlds$carry(lap);
        ((NavigationShifter) mob.getLookControl()).exotic_worlds$carry(lap);
    }

    private SeamSnap() {
    }
}
