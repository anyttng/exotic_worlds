package com.exoticworlds.compat.create;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;

public final class CanonicalPositionKeys {
    public static Set<BlockPos> set(@Nullable BlockGetter reader, Set<BlockPos> built) {
        WorldFold fold = wrappedFoldOf(reader);
        return fold == null ? built : new CanonicalSet(fold);
    }

    static @Nullable WorldFold wrappedFoldOf(@Nullable BlockGetter reader) {
        return reader instanceof LevelReader levelReader ? WorldLoopAttachments.wrappedFoldOfReader(levelReader) : null;
    }

    private static Object key(WorldFold fold, Object candidate) {
        return candidate instanceof BlockPos pos ? fold.fold(pos) : candidate;
    }

    abstract static class FoldedSet<E> extends AbstractSet<E> {
        final WorldFold fold;
        final Set<E> delegate;

        FoldedSet(WorldFold fold, Set<E> delegate) {
            this.fold = fold;
            this.delegate = delegate;
        }

        abstract Object canonical(Object candidate);

        @Override
        public final boolean contains(Object candidate) {
            return delegate.contains(canonical(candidate));
        }

        @Override
        public final boolean containsAll(Collection<?> candidates) {
            for (Object candidate : candidates) {
                if (!delegate.contains(canonical(candidate))) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public final boolean remove(Object candidate) {
            return delegate.remove(canonical(candidate));
        }

        @Override
        public final boolean removeAll(Collection<?> candidates) {
            boolean changed = false;
            for (Object candidate : candidates) {
                changed |= delegate.remove(canonical(candidate));
            }

            return changed;
        }

        @Override
        public final boolean retainAll(Collection<?> candidates) {
            Set<Object> kept = new HashSet<>();
            for (Object candidate : candidates) {
                kept.add(canonical(candidate));
            }

            return delegate.removeIf(element -> !kept.contains(element));
        }

        @Override
        public final Iterator<E> iterator() {
            return delegate.iterator();
        }

        @Override
        public final int size() {
            return delegate.size();
        }

        @Override
        public final void clear() {
            delegate.clear();
        }
    }

    static final class CanonicalSet extends FoldedSet<BlockPos> {
        CanonicalSet(WorldFold fold) {
            super(fold, new HashSet<>());
        }

        @Override
        Object canonical(Object candidate) {
            return key(fold, candidate);
        }

        @Override
        public boolean add(BlockPos pos) {
            return delegate.add(fold.fold(pos));
        }

        @Override
        public boolean addAll(Collection<? extends BlockPos> positions) {
            boolean changed = false;
            for (BlockPos pos : positions) {
                changed |= delegate.add(fold.fold(pos));
            }

            return changed;
        }
    }

    private CanonicalPositionKeys() {
    }
}
