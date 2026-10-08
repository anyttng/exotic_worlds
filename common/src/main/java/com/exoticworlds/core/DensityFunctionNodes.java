package com.exoticworlds.core;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;

public final class DensityFunctionNodes {
    public static void forEach(DensityFunction root, Consumer<DensityFunction> visitor) {
        first(root, node -> {
            visitor.accept(node);
            return false;
        });
    }

    public static @Nullable DensityFunction first(DensityFunction root, Predicate<DensityFunction> match) {
        Deque<DensityFunction> pending = new ArrayDeque<>();
        Set<DensityFunction> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        pending.add(root);
        seen.add(root);
        while (!pending.isEmpty()) {
            DensityFunction node = pending.remove();
            if (match.test(node)) {
                return node;
            }

            if (node instanceof DensityFunctions.HolderHolder(Holder<DensityFunction> holder) && !holder.isBound()) {
                continue;
            }

            node.rewriteChildren(child -> {
                if (seen.add(child)) {
                    pending.add(child);
                }

                return child;
            });
        }

        return null;
    }

    private DensityFunctionNodes() {
    }
}
