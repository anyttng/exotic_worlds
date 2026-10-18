package com.exoticworlds.engine.seam;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.NavigationShifter;
import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.SeamTransform;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.mixin.EntityAccessor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.phys.Vec3;

public final class SeamSnap {
    public static boolean intoBounds(Entity entity) {
        WorldFold fold = WorldLoopAttachments.wrappedTransformerOf(entity.level());
        if (fold == null) {
            return false;
        }

        DeckTransformation lap = fold.foldTransformation(entity.position());
        if (lap.isIdentity()) {
            return false;
        }

        withPassengers(entity, lap);
        return true;
    }

    // mc/1.21: Create's carriage moves its riders by the carriage's own lap.
    public static void withPassengers(Entity entity, DeckTransformation lap) {
        SeamTransform applied = lap.blocks();
        Vec3 from = entity.position();
        Vec3 to = lap.apply(from);
        if (entity instanceof BlockAttachedEntity attached) {
            attached.setPos(Vec3.atCenterOf(lap.apply(attached.getPos())));
        } else {
            entity.setPos(to.x, to.y, to.z);
        }

        entity.xo = applied.applyX(entity.xo);
        entity.zo = applied.applyZ(entity.zo);
        entity.xOld = applied.applyX(entity.xOld);
        entity.zOld = applied.applyZ(entity.zOld);
        EntityAccessor stored = (EntityAccessor) entity;
        @Nullable Vec3 known = stored.exotic_worlds$lastKnownPosition();
        if (known != null) {
            stored.exotic_worlds$setLastKnownPosition(lap.apply(known));
        }

        if (entity instanceof Mob mob) {
            int shiftX = (int) Math.round(to.x - from.x);
            int shiftZ = (int) Math.round(to.z - from.z);
            ((NavigationShifter) mob.getNavigation()).exotic_worlds$shiftBy(shiftX, shiftZ);
            ((NavigationShifter) mob.getMoveControl()).exotic_worlds$shiftBy(shiftX, shiftZ);
            ((NavigationShifter) mob.getLookControl()).exotic_worlds$shiftBy(shiftX, shiftZ);
        }

        if (entity instanceof ServerPlayer rider) {
            rider.level().getChunkSource().move(rider);
        }

        for (Entity passenger : entity.getPassengers()) {
            withPassengers(passenger, lap);
        }
    }

    private SeamSnap() {
    }
}
