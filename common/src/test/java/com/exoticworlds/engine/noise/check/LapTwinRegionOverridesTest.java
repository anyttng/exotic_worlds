package com.exoticworlds.engine.noise.check;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import net.minecraft.server.level.WorldGenRegion;

class LapTwinRegionOverridesTest {
    private static final Set<String> ANSWERED_BY_VANILLA = Set.of(
            "isOldChunkAround", "getCenter", "setCurrentlyGenerating", "getChunk(int,int)",
            "getBiomeManager", "registryAccess", "dimensionType", "environmentAttributes");

    @Test
    void everyVanillaRegionMethodOutsideTheReadOnlyListIsOurOwn() {
        Set<String> ours = signaturesOf(LapTwinRegion.class);
        List<String> missing = signaturesOf(WorldGenRegion.class).stream()
                .filter(signature -> !ours.contains(signature))
                .filter(signature -> !ANSWERED_BY_VANILLA.contains(signature)
                        && !ANSWERED_BY_VANILLA.contains(nameOf(signature)))
                .sorted()
                .toList();
        assertEquals(List.of(), missing);
    }

    @Test
    void everyReadOnlyEntryStillNamesAVanillaRegionMethod() {
        Set<String> vanilla = signaturesOf(WorldGenRegion.class);
        Set<String> vanillaNames = vanilla.stream().map(LapTwinRegionOverridesTest::nameOf).collect(Collectors.toSet());
        List<String> stale = ANSWERED_BY_VANILLA.stream()
                .filter(entry -> !vanilla.contains(entry) && !vanillaNames.contains(entry))
                .sorted()
                .toList();
        assertEquals(List.of(), stale);
    }

    private static Set<String> signaturesOf(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(method -> !method.isSynthetic() && !method.isBridge())
                .filter(method -> !Modifier.isStatic(method.getModifiers()) && !Modifier.isPrivate(method.getModifiers()))
                .map(LapTwinRegionOverridesTest::signatureOf)
                .collect(Collectors.toSet());
    }

    private static String signatureOf(Method method) {
        return method.getName() + Arrays.stream(method.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(",", "(", ")"));
    }

    private static String nameOf(String signature) {
        return signature.substring(0, signature.indexOf('('));
    }
}
