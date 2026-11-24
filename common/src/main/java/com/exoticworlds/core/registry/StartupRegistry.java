package com.exoticworlds.core.registry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BinaryOperator;

public final class StartupRegistry<K, V> {
    private final String subject;

    private volatile Map<K, V> entries = Map.of();

    private boolean isClosed;

    public StartupRegistry(String subject) {
        this(RegistrationBoundary.STARTUP, subject);
    }

    StartupRegistry(RegistrationBoundary boundary, String subject) {
        this.subject = subject;
        boundary.register(this);
    }

    public synchronized void register(K key, V value) {
        requireOpen();
        if (entries.containsKey(key)) {
            throw new IllegalStateException(subject + " already holds " + key + "; a second registration under it"
                    + " would replace the first.");
        }

        put(key, value);
    }

    public synchronized void merge(K key, V value, BinaryOperator<V> merger) {
        requireOpen();
        V held = entries.get(key);
        put(key, held == null ? value : merger.apply(held, value));
    }

    private void requireOpen() {
        if (isClosed) {
            throw new IllegalStateException(subject + " must be registered before the server starts.");
        }
    }

    private void put(K key, V value) {
        Map<K, V> grown = new LinkedHashMap<>(entries);
        grown.put(key, value);
        entries = Collections.unmodifiableMap(grown);
    }

    public Map<K, V> getEntries() {
        return entries;
    }

    synchronized void close() {
        isClosed = true;
    }
}
