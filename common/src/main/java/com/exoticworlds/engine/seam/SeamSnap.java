package com.exoticworlds.engine.seam;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.NavigationShifter;
import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.SeamTransform;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.mixin.EntityAccessor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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
        Vec3 from = entity.position();
        Vec3 to = lap.apply(from);
        moveBody(entity, lap, to);
        moveHistory(entity, lap);
        if (entity instanceof Mob mob) {
            shiftNavigation(mob, (int) Math.round(to.x - from.x), (int) Math.round(to.z - from.z));
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
        entity.xo = applied.applyX(entity.xo);
        entity.zo = applied.applyZ(entity.zo);
        entity.xOld = applied.applyX(entity.xOld);
        entity.zOld = applied.applyZ(entity.zOld);
        EntityAccessor stored = (EntityAccessor) entity;
        @Nullable Vec3 known = stored.exotic_worlds$lastKnownPosition();
        if (known != null) {
            stored.exotic_worlds$setLastKnownPosition(lap.apply(known));
        }
    }

    private static void shiftNavigation(Mob mob, int shiftX, int shiftZ) {
        ((NavigationShifter) mob.getNavigation()).exotic_worlds$shiftBy(shiftX, shiftZ);
        ((NavigationShifter) mob.getMoveControl()).exotic_worlds$shiftBy(shiftX, shiftZ);
        ((NavigationShifter) mob.getLookControl()).exotic_worlds$shiftBy(shiftX, shiftZ);
    }

    private SeamSnap() {
    }
}
