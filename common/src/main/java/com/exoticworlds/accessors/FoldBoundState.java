package com.exoticworlds.accessors;

import java.util.function.Supplier;

public interface FoldBoundState {
    <T> T exotic_worlds$inFold(Supplier<T> action);
}
