package com.exoticworlds.core.registry;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import com.exoticworlds.ExoticWorlds;

public final class NamespaceOrder {

    public static <T> List<T> sorted(Collection<T> registered, Function<T, String> namespace) {
        return registered.stream().sorted(Comparator.comparing(namespace, NamespaceOrder::compare)).toList();
    }

    private static int compare(String first, String second) {
        boolean isFirstOwn = ExoticWorlds.MODID.equals(first);
        boolean isSecondOwn = ExoticWorlds.MODID.equals(second);
        return isFirstOwn || isSecondOwn ? Boolean.compare(isSecondOwn, isFirstOwn) : first.compareTo(second);
    }

    private NamespaceOrder() {
    }
}
