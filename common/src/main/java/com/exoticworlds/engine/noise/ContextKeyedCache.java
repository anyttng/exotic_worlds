package com.exoticworlds.engine.noise;

import java.util.Arrays;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationTransformerContext.Context;

public final class ContextKeyedCache<T> {
    static final int CAPACITY = 8;

    private static final Entry<?>[] EMPTY = new Entry<?>[0];

    private record Entry<T>(WorldFold fold, SlotAxes axes, double scale, double verticalShare, double xDivisor,
            double zDivisor, T value) {
        boolean matches(WorldFold transformer, Context context) {
            return this.fold == transformer
                    && this.scale == context.horizontalScale()
                    && this.verticalShare == context.verticalShare()
                    && this.xDivisor == context.xDivisor()
                    && this.zDivisor == context.zDivisor()
                    && this.axes.equals(context.slotAxes());
        }
    }

    @SuppressWarnings("unchecked")
    private volatile Entry<T>[] entries = (Entry<T>[]) EMPTY;

    public @Nullable T find(WorldFold transformer, Context context) {
        for (Entry<T> entry : this.entries) {
            if (entry.matches(transformer, context)) {
                return entry.value();
            }
        }

        return null;
    }

    public T put(WorldFold transformer, Context context, T value) {
        Entry<T>[] current = this.entries;
        int kept = Math.min(current.length, CAPACITY - 1);
        Entry<T>[] next = Arrays.copyOfRange(current, current.length - kept, current.length + 1);
        next[kept] = new Entry<>(transformer, context.slotAxes(), context.horizontalScale(), context.verticalShare(),
                context.xDivisor(), context.zDivisor(), value);
        this.entries = next;
        return value;
    }
}
