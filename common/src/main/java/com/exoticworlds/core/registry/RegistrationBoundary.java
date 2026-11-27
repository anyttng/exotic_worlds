package com.exoticworlds.core.registry;

import java.util.ArrayList;
import java.util.List;

public final class RegistrationBoundary {
    public static final RegistrationBoundary STARTUP = new RegistrationBoundary();

    private final List<StartupRegistry<?, ?>> registered = new ArrayList<>();

    private final List<Runnable> atFirstClose = new ArrayList<>();

    private boolean isClosed;

    RegistrationBoundary() {
    }

    synchronized void register(StartupRegistry<?, ?> registry) {
        if (isClosed) {
            registry.close();
            return;
        }

        registered.add(registry);
    }

    public synchronized void atFirstClose(Runnable check) {
        atFirstClose.add(check);
    }

    public synchronized void close() {
        boolean isFirst = !isClosed;
        isClosed = true;
        registered.forEach(StartupRegistry::close);
        if (isFirst) {
            atFirstClose.forEach(Runnable::run);
        }
    }
}
