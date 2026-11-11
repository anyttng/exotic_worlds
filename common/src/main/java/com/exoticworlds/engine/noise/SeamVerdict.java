package com.exoticworlds.engine.noise;

import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record SeamVerdict(ResourceKey<Level> dimension, List<Break> breaks,
        SeamCause.@Nullable Generator unchecked) {
    private static final String FINGERPRINT_SEPARATOR = "|";
    private static final String LIST_SEPARATOR = ",";
    private static final String CAUSE_SEPARATOR = "=";

    public record Break(String field, SeamCause cause) {
    }

    public boolean broken() {
        return !this.breaks.isEmpty();
    }

    public String fields() {
        return this.breaks.stream().map(Break::field).collect(Collectors.joining(LIST_SEPARATOR));
    }

    public String causes() {
        return this.breaks.stream()
                .map(broken -> broken.field() + CAUSE_SEPARATOR + broken.cause().key())
                .collect(Collectors.joining(LIST_SEPARATOR));
    }

    public String fingerprint() {
        return this.dimension.location() + FINGERPRINT_SEPARATOR + this.causes();
    }
}
