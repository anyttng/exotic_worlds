package com.exoticworlds.core;

import java.util.ArrayList;
import java.util.List;

public final class RegistrationBoundary {
    public static final RegistrationBoundary STARTUP = new RegistrationBoundary();

    private final List<StartupRegistry<?, ?>> registered = new ArrayList<>();

    private final List<Runnable> atFirstClose = new ArrayList<>();

    private boolean closed;

    RegistrationBoundary() {
    }

    synchronized void register(StartupRegistry<?, ?> registry) {
        if (closed) {
            registry.close();
            return;
        }

        registered.add(registry);
    }

    public synchronized void atFirstClose(Runnable check) {
        atFirstClose.add(check);
    }

    public synchronized void close() {
        boolean first = !closed;
        closed = true;
        registered.forEach(StartupRegistry::close);
        if (first) {
            atFirstClose.forEach(Runnable::run);
        }
    }
}
