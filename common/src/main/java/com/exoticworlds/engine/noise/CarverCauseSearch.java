package com.exoticworlds.engine.noise;

import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.carver.WorldCarver;

final class CarverCauseSearch {
    private static final String VANILLA_PACKAGE = "net.minecraft.";
    private static final String CARVER_CLASS = WorldCarver.class.getName();

    static SeamCause.@Nullable Node causeOf(IdentityHashMap<ConfiguredWorldCarver<?>, BitSet> here,
            IdentityHashMap<ConfiguredWorldCarver<?>, BitSet> there, Registry<ConfiguredWorldCarver<?>> carvers) {
        Set<ConfiguredWorldCarver<?>> all = Collections.newSetFromMap(new IdentityHashMap<>());
        all.addAll(here.keySet());
        all.addAll(there.keySet());
        return all.stream()
                .filter(carver -> !Objects.equals(here.get(carver), there.get(carver)))
                .map(carver -> nodeOf(carver, carvers))
                .min(Comparator.comparing(SeamCause.Node::vanilla).thenComparing(SeamCause.Node::type))
                .orElse(null);
    }

    private static SeamCause.Node nodeOf(ConfiguredWorldCarver<?> carver, Registry<ConfiguredWorldCarver<?>> carvers) {
        ResourceLocation type = BuiltInRegistries.CARVER.getKey(carver.worldCarver());
        ResourceLocation holderKey = carvers.getKey(carver);
        @Nullable String holder = holderKey != null ? holderKey.toString() : null;
        String className = carver.worldCarver().getClass().getName();
        boolean vanilla = type != null
                ? type.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)
                : className.startsWith(VANILLA_PACKAGE);
        List<String> candidates = vanilla
                ? AppliedMixins.foreignOn(List.of(className, CARVER_CLASS))
                : List.of();
        return new SeamCause.Node(type != null ? type.toString() : className, holder,
                type != null ? type.getNamespace() : null, vanilla, candidates);
    }

    private CarverCauseSearch() {
    }
}
