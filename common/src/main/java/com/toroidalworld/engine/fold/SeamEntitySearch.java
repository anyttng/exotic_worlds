package com.toroidalworld.engine.fold;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFold.Folded;

import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.phys.AABB;

public final class SeamEntitySearch {
    public static AABB sectionReach(AABB box) {
        return box.inflate(EntitySectionStorage.CHONKY_ENTITY_SEARCH_GRACE, 0.0,
                EntitySectionStorage.CHONKY_ENTITY_SEARCH_GRACE);
    }

    public static boolean crossesSeam(WorldFold fold, AABB reach) {
        return fold.isWrapped() && fold.crossesBounds(reach);
    }

    public static <E extends Entity> List<E> collect(WorldFold fold, AABB box, Function<AABB, List<E>> walk) {
        AABB reach = sectionReach(box);
        if (!crossesSeam(fold, reach)) {
            return walk.apply(box);
        }

        Predicate<Entity> firstOverlap = firstOverlap(fold, box);
        List<E> found = new ArrayList<>();
        for (Folded<AABB> piece : fold.split(reach)) {
            for (E entity : walk.apply(piece.value())) {
                if (firstOverlap.test(entity)) {
                    found.add(entity);
                }
            }
        }

        return found;
    }

    public static <E extends Entity> void forEach(WorldFold fold, AABB box, Consumer<E> output,
            BiConsumer<AABB, Consumer<E>> walk) {
        AABB reach = sectionReach(box);
        if (!crossesSeam(fold, reach)) {
            walk.accept(box, output);
            return;
        }

        Predicate<Entity> firstOverlap = firstOverlap(fold, box);
        Consumer<E> once = entity -> {
            if (firstOverlap.test(entity)) {
                output.accept(entity);
            }
        };
        for (Folded<AABB> piece : fold.split(reach)) {
            walk.accept(piece.value(), once);
        }
    }

    public static <E extends Entity> void forEachUntilAborted(WorldFold fold, AABB box,
            AbortableIterationConsumer<E> output, BiConsumer<AABB, AbortableIterationConsumer<E>> walk) {
        AABB reach = sectionReach(box);
        if (!crossesSeam(fold, reach)) {
            walk.accept(box, output);
            return;
        }

        Predicate<Entity> firstOverlap = firstOverlap(fold, box);
        MutableBoolean aborted = new MutableBoolean();
        AbortableIterationConsumer<E> once = entity -> {
            if (!firstOverlap.test(entity)) {
                return AbortableIterationConsumer.Continuation.CONTINUE;
            }

            AbortableIterationConsumer.Continuation next = output.accept(entity);
            if (next.shouldAbort()) {
                aborted.setTrue();
            }

            return next;
        };
        for (Folded<AABB> piece : fold.split(reach)) {
            walk.accept(piece.value(), once);
            if (aborted.isTrue()) {
                return;
            }
        }
    }

    private static Predicate<Entity> firstOverlap(WorldFold fold, AABB box) {
        Set<Entity> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        return entity -> fold.boxesOverlap(box, entity.getBoundingBox()) && seen.add(entity);
    }

    private SeamEntitySearch() {
    }
}
