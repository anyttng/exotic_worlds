package com.exoticworlds.core.feature;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NoiseOffsets {
    private final Map<String, Cell> cells = new ConcurrentHashMap<>();

    private static final class Cell implements NoiseOffset {
        private volatile double value;

        @Override
        public double get() {
            return this.value;
        }
    }

    public void set(String key, double offset) {
        cellOf(key).value = offset;
    }

    public NoiseOffset sumOf(List<String> keys) {
        List<Cell> summed = keys.stream().map(this::cellOf).toList();
        if (summed.isEmpty()) {
            return NoiseOffset.NONE;
        }

        if (summed.size() == 1) {
            return summed.getFirst();
        }

        return () -> {
            double sum = 0.0;
            for (Cell cell : summed) {
                sum += cell.value;
            }
            return sum;
        };
    }

    private Cell cellOf(String key) {
        return this.cells.computeIfAbsent(key, _ -> new Cell());
    }
}
