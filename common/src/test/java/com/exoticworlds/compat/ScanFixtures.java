package com.exoticworlds.compat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.fixture.mod.FixtureModApi;
import com.exoticworlds.compat.fixture.mod.FixtureModBefore;
import com.exoticworlds.compat.fixture.mod.FixtureModLater;
import com.exoticworlds.compat.fixture.mod.FixtureModThing;
import com.exoticworlds.compat.fixture.scan.FixtureCaller;
import com.exoticworlds.compat.fixture.scan.FixtureOptionalHolder;
import com.exoticworlds.compat.fixture.scan.FixtureScanPlugin;
import com.exoticworlds.compat.fixture.scan.nested.FixtureNestedCaller;
import com.exoticworlds.compat.fixture.scan.nested.FixtureNestedPlugin;
import com.exoticworlds.compat.fixture.shared.FixtureSharedFirst;
import com.exoticworlds.compat.fixture.shared.FixtureSharedPlugin;
import com.exoticworlds.compat.fixture.shared.FixtureSharedSecond;
import com.exoticworlds.compat.fixture.shared.mixin.FixtureSharedFirstMixin;
import com.exoticworlds.compat.fixture.shared.mixin.FixtureSharedSecondMixin;

final class ScanFixtures {
    static final String MOD_PACKAGE = internalName(FixtureModApi.class.getPackageName()) + "/";
    static final String SCAN_CONFIG = "fixture.scan.mixins.json";
    static final String NESTED_CONFIG = "fixture.nested.mixins.json";
    static final String SHARED_CONFIG = "fixture.shared.mixins.json";
    static final String OPTIONAL_HOLDER = FixtureOptionalHolder.class.getSimpleName();
    static final String SHARED_FIRST = FixtureSharedFirst.class.getSimpleName();
    static final String SHARED_FIRST_MIXIN = FixtureSharedFirstMixin.class.getSimpleName();
    static final String SHARED_SECOND_MIXIN = FixtureSharedSecondMixin.class.getSimpleName();

    private static final String CLASS_SUFFIX = ".class";
    private static final List<Class<?>> LISTED = List.of(FixtureScanPlugin.class, FixtureCaller.class,
            FixtureOptionalHolder.class, FixtureNestedPlugin.class, FixtureNestedCaller.class,
            FixtureSharedPlugin.class, FixtureSharedFirst.class, FixtureSharedSecond.class,
            FixtureSharedFirstMixin.class, FixtureSharedSecondMixin.class);

    private ScanFixtures() {
    }

    static ClassLoader current() {
        return new FixtureModLoader(Map.of(), Set.of(), true);
    }

    static ClassLoader withoutLater() {
        return new FixtureModLoader(Map.of(), Set.of(resource(FixtureModLater.class)), true);
    }

    static ClassLoader older() {
        return new FixtureModLoader(Map.of(resource(FixtureModApi.class), resource(FixtureModBefore.class)),
                Set.of(resource(FixtureModThing.class)), true);
    }

    static ClassLoader withoutIndex() {
        return new FixtureModLoader(Map.of(), Set.of(), false);
    }

    static String internalName(Class<?> type) {
        return internalName(type.getName());
    }

    private static String internalName(String name) {
        return name.replace('.', '/');
    }

    private static String resource(Class<?> type) {
        return internalName(type) + CLASS_SUFFIX;
    }

    private static final class FixtureModLoader extends ClassLoader {
        private final Map<String, String> swapped;
        private final Set<String> hidden;
        private final boolean indexed;

        private FixtureModLoader(Map<String, String> swapped, Set<String> hidden, boolean indexed) {
            super(ScanFixtures.class.getClassLoader());
            this.swapped = swapped;
            this.hidden = hidden;
            this.indexed = indexed;
        }

        @Override
        public @Nullable InputStream getResourceAsStream(String name) {
            if (name.equals(ModPresence.CLASS_INDEX)) {
                return this.indexed ? new ByteArrayInputStream(LISTED.stream().map(ScanFixtures::internalName)
                        .collect(Collectors.joining("\n")).getBytes(StandardCharsets.UTF_8)) : null;
            }

            if (this.hidden.contains(name)) {
                return null;
            }

            return super.getResourceAsStream(this.swapped.getOrDefault(name, name));
        }
    }
}
