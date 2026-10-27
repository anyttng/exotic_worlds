package com.exoticworlds.compat.create;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;

public final class CanonicalPositionKeys {
    public static Set<BlockPos> set(@Nullable BlockGetter reader) {
        WorldFold fold = wrappedFoldOf(reader);
        return fold == null ? new HashSet<>() : new CanonicalSet(fold);
    }

    public static <V> Map<BlockPos, V> map(@Nullable BlockGetter reader) {
        WorldFold fold = wrappedFoldOf(reader);
        return fold == null ? new HashMap<>() : new CanonicalMap<>(fold);
    }

    static @Nullable WorldFold wrappedFoldOf(@Nullable BlockGetter reader) {
        return reader instanceof LevelReader levelReader ? WorldLoopAttachments.wrappedTransformerOfReader(levelReader) : null;
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
            this(fold, new HashSet<>());
        }

        private CanonicalSet(WorldFold fold, Set<BlockPos> keys) {
            super(fold, keys);
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

    private static final class CanonicalEntrySet<V> extends FoldedSet<Map.Entry<BlockPos, V>> {
        private CanonicalEntrySet(WorldFold fold, Set<Map.Entry<BlockPos, V>> entries) {
            super(fold, entries);
        }

        @Override
        Object canonical(Object candidate) {
            if (candidate instanceof Map.Entry<?, ?> entry && entry.getKey() instanceof BlockPos pos) {
                return new AbstractMap.SimpleEntry<>(fold.fold(pos), entry.getValue());
            }

            return candidate;
        }
    }

    static final class CanonicalMap<V> extends AbstractMap<BlockPos, V> {
        private final WorldFold fold;
        private final Map<BlockPos, V> entries = new HashMap<>();
        private final Set<BlockPos> keys;
        private final Set<Entry<BlockPos, V>> foldedEntries;

        CanonicalMap(WorldFold fold) {
            this.fold = fold;
            this.keys = new CanonicalSet(fold, entries.keySet());
            this.foldedEntries = new CanonicalEntrySet<>(fold, entries.entrySet());
        }

        @Override
        public @Nullable V put(BlockPos pos, V value) {
            return entries.put(fold.fold(pos), value);
        }

        @Override
        public @Nullable V get(Object candidate) {
            return entries.get(key(fold, candidate));
        }

        @Override
        public boolean containsKey(Object candidate) {
            return entries.containsKey(key(fold, candidate));
        }

        @Override
        public @Nullable V remove(Object candidate) {
            return entries.remove(key(fold, candidate));
        }

        @Override
        public Set<BlockPos> keySet() {
            return keys;
        }

        @Override
        public Collection<V> values() {
            return entries.values();
        }

        @Override
        public Set<Entry<BlockPos, V>> entrySet() {
            return foldedEntries;
        }

        @Override
        public int size() {
            return entries.size();
        }

        @Override
        public void clear() {
            entries.clear();
        }
    }

    private CanonicalPositionKeys() {
    }
}
