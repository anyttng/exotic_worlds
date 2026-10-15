package com.exoticworlds.engine.fold;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.CoordinateConstants;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.phys.AABB;

class SeamEntitySearchTest {
    private static final int MIN_CHUNK = -8;
    private static final int MAX_CHUNK = 8;
    private static final int LOWER = MIN_CHUNK * CoordinateConstants.CHUNK_WIDTH;
    private static final int UPPER = MAX_CHUNK * CoordinateConstants.CHUNK_WIDTH;

    private static final double MIN_Y = 60.0;
    private static final double MAX_Y = 62.0;
    private static final double MIN_Z = 4.0;
    private static final double MAX_Z = 6.0;

    private static final double QUERY_SIDE = 1.0;
    private static final double SHORT_OF_THE_BOUND = 0.05;
    private static final double HALF_WIDTH = 0.25;
    private static final double PAST_THE_BOUND = 0.1;
    private static final double INSIDE_THE_REACH = 1.0;

    private static final WorldFold CYLINDER = WorldFolds.of(FlatShape.cylinder(
            new WorldLoopBounds(new AxisBounds.Looped(MIN_CHUNK, MAX_CHUNK), AxisBounds.Unbounded.INSTANCE)));

    private static final AABB SHORT_OF_THE_UPPER_BOUND = new AABB(UPPER - QUERY_SIDE, MIN_Y, MIN_Z,
            UPPER - SHORT_OF_THE_BOUND, MAX_Y, MAX_Z);

    @Test
    void anEntityReachingAcrossTheBoundFromJustPastItIsFound() {
        Stub acrossTheBound = standingAt(LOWER + PAST_THE_BOUND);

        assertEquals(List.of(acrossTheBound), forEach(SHORT_OF_THE_UPPER_BOUND, acrossTheBound));
    }

    @Test
    void anAbortableSearchFindsTheEntityReachingAcrossTheBound() {
        Stub acrossTheBound = standingAt(LOWER + PAST_THE_BOUND);

        assertEquals(List.of(acrossTheBound), forEachUntilAborted(SHORT_OF_THE_UPPER_BOUND, acrossTheBound));
    }

    @Test
    void anEntityTheReachMeetsAndTheBoxDoesNotIsLeftOut() {
        Stub inTheReachOnly = standingAt(LOWER + INSIDE_THE_REACH);

        assertEquals(List.of(), forEach(SHORT_OF_THE_UPPER_BOUND, inTheReachOnly));
    }

    private static List<Stub> forEach(AABB box, Stub... world) {
        List<Stub> found = new ArrayList<>();
        SeamEntitySearch.<Stub>forEach(CYLINDER, box, found::add,
                (piece, sink) -> rawMeeting(piece, world).forEach(sink));
        return found;
    }

    private static List<Stub> forEachUntilAborted(AABB box, Stub... world) {
        List<Stub> found = new ArrayList<>();
        SeamEntitySearch.<Stub>forEachUntilAborted(CYLINDER, box, entity -> {
            found.add(entity);
            return AbortableIterationConsumer.Continuation.CONTINUE;
        }, (piece, sink) -> rawMeeting(piece, world).forEach(sink::accept));
        return found;
    }

    private static List<Stub> rawMeeting(AABB piece, Stub... world) {
        return Stream.of(world).filter(entity -> entity.getBoundingBox().intersects(piece)).toList();
    }

    private static Stub standingAt(double x) {
        double z = (MIN_Z + MAX_Z) / 2.0;
        return new Stub(new AABB(x - HALF_WIDTH, MIN_Y, z - HALF_WIDTH, x + HALF_WIDTH, MAX_Y, z + HALF_WIDTH));
    }

    private record Stub(AABB box) implements EntityAccess {
        @Override
        public AABB getBoundingBox() {
            return this.box;
        }

        @Override
        public int getId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public UUID getUUID() {
            throw new UnsupportedOperationException();
        }

        @Override
        public BlockPos blockPosition() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setLevelCallback(EntityInLevelCallback callback) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Stream<? extends EntityAccess> getSelfAndPassengers() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Stream<? extends EntityAccess> getPassengersAndSelf() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setRemoved(Entity.RemovalReason reason) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean shouldBeSaved() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAlwaysTicking() {
            throw new UnsupportedOperationException();
        }
    }
}
