package com.exoticworlds.engine;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

public final class ThreadScope<T> {
    @SuppressWarnings("ThreadLocalUsage")
    private final ThreadLocal<@Nullable T> bound = new ThreadLocal<>();

    public <R> R with(@Nullable T value, Supplier<R> body) {
        T previous = enter(value);
        try {
            return body.get();
        } finally {
            exit(previous);
        }
    }

    public @Nullable T enter(@Nullable T value) {
        T previous = bound.get();
        rebind(value);
        return previous;
    }

    public void exit(@Nullable T previous) {
        rebind(previous);
    }

    public @Nullable T current() {
        return bound.get();
    }

    private void rebind(@Nullable T value) {
        if (value == null) {
            bound.remove();
        } else {
            bound.set(value);
        }
    }
}
