package com.exoticworlds.compat.gate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.base.Splitter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class CompatGatesTest {
    private static final String REPO_ROOT_PROPERTY = "toroidal.repoRoot";
    private static final String RESOURCES = "common/src/main/resources";
    private static final String COMPAT_CONFIGS = "exotic_worlds.compat.*.mixins.json";
    private static final String PLUGIN_KEY = "plugin";
    private static final String PACKAGE_KEY = "package";
    private static final List<String> MIXIN_LIST_KEYS = List.of("mixins", "client", "server");
    private static final String GATE_PACKAGE = "com/exoticworlds/compat/gate/";
    private static final String SHIPPED_CLASS = "ModPresence.class";
    private static final String CLASS_SUFFIX = ".class";
    private static final String BETTER_END_CONFIG = "exotic_worlds.compat.betterend.mixins.json";
    private static final Set<String> ORIGINAL_BETTER_END_CONFIGS =
            Set.of("exotic_worlds.compat.betterend.ambertation.mixins.json");
    private static final String GET_GATES_METHOD = "getGates";
    private static final String IS_ADMITTED_METHOD = "isAdmitted";

    private static final class OriginalBuildLoader extends URLClassLoader {
        private static final String JARS_PROPERTY = "toroidal.betterEndOriginal";
        private static final List<String> OWNED_PACKAGES =
                List.of("org/betterx/betterend/", "org/betterx/bclib/", "com/exoticworlds/");

        static {
            registerAsParallelCapable();
        }

        private OriginalBuildLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        static OriginalBuildLoader ofOriginalBetterEnd() throws Exception {
            String jars = System.getProperty(JARS_PROPERTY);
            assertNotNull(jars, JARS_PROPERTY + " names no jar: the suite runs from common's test task");
            List<URL> urls = new ArrayList<>();
            for (String jar : Splitter.on(File.pathSeparatorChar).omitEmptyStrings().split(jars)) {
                urls.add(Path.of(jar).toUri().toURL());
            }

            urls.add(ModPresence.class.getProtectionDomain().getCodeSource().getLocation());
            return new OriginalBuildLoader(urls.toArray(URL[]::new), CompatGatesTest.class.getClassLoader());
        }

        private static boolean isOwned(String internalName) {
            return OWNED_PACKAGES.stream().anyMatch(internalName::startsWith);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!isOwned(name.replace('.', '/'))) {
                return super.loadClass(name, resolve);
            }

            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    loaded = findClass(name);
                }

                if (resolve) {
                    resolveClass(loaded);
                }

                return loaded;
            }
        }

        @Override
        public @Nullable URL getResource(String name) {
            return isOwned(name) ? findResource(name) : super.getResource(name);
        }
    }

    static List<Path> configs() throws IOException {
        List<Path> configs = new ArrayList<>();
        try (DirectoryStream<Path> found = Files.newDirectoryStream(
                Path.of(System.getProperty(REPO_ROOT_PROPERTY)).resolve(RESOURCES), COMPAT_CONFIGS)) {
            found.forEach(configs::add);
        }

        assertTrue(!configs.isEmpty(), "no compat config found under " + RESOURCES);
        return configs;
    }

    @ParameterizedTest
    @MethodSource("configs")
    void everyMixinOfTheConfigHasExactlyOneGate(Path config) throws Exception {
        JsonObject json = read(config);
        ModPresenceGatePlugin plugin = pluginOf(json);
        String mixinPackage = json.get(PACKAGE_KEY).getAsString();

        for (String mixin : mixinsOf(json)) {
            String mixinClass = mixinPackage + "." + mixin;
            long gates = plugin.getGates().stream().filter(gate -> gate.isCovering(mixinClass)).count();
            assertEquals(1, gates, config.getFileName() + ": " + mixin + " is covered by " + gates + " gates");
        }
    }

    @ParameterizedTest
    @MethodSource("configs")
    void everyGateAdmitsTheModBuildTheSuiteCarries(Path config) throws Exception {
        JsonObject json = read(config);
        if (ORIGINAL_BETTER_END_CONFIGS.contains(config.getFileName().toString())) {
            assertAdmittedOnTheOriginalBetterEnd(json, config);
            return;
        }

        for (ModPresence gate : pluginOf(json).getGates()) {
            assertTrue(gate.isAdmitted(), config.getFileName() + ": a gate refuses the build this suite runs against; "
                    + "the refusal line in the test log names the mixin and the member");
        }
    }

    @Test
    void theSharedBetterEndGateAdmitsTheOriginalBuildToo() throws Exception {
        Path config = Path.of(System.getProperty(REPO_ROOT_PROPERTY)).resolve(RESOURCES).resolve(BETTER_END_CONFIG);
        assertAdmittedOnTheOriginalBetterEnd(read(config), config);
    }

    private static void assertAdmittedOnTheOriginalBetterEnd(JsonObject json, Path config) throws Exception {
        try (OriginalBuildLoader loader = OriginalBuildLoader.ofOriginalBetterEnd()) {
            Object plugin = Class.forName(json.get(PLUGIN_KEY).getAsString(), true, loader)
                    .getDeclaredConstructor().newInstance();
            for (Object gate : (List<?>) plugin.getClass().getMethod(GET_GATES_METHOD).invoke(plugin)) {
                assertTrue((Boolean) gate.getClass().getMethod(IS_ADMITTED_METHOD).invoke(gate),
                        config.getFileName() + ": a gate refuses the original BetterEnd build; the refusal line in"
                                + " the test log names the mixin and the member");
            }
        }
    }

    @Test
    void theClassIndexListsEveryCompiledClassOfAPluginPackage() throws Exception {
        assertEquals(compiledPluginClasses(), indexedClasses(),
                "the jar's class index and the compiled packages of the compat plugins hold the same classes");
    }

    @Test
    void everyClassOfAGatePackageBelongsToExactlyOneGate() throws Exception {
        List<Set<String>> owned = new ArrayList<>();
        for (Path config : configs()) {
            for (ModPresence gate : pluginOf(read(config)).getGates()) {
                owned.add(Set.copyOf(gate.scanOwnedClasses()));
            }
        }

        for (String listed : indexedClasses()) {
            String className = listed.replace('/', '.');
            long owners = owned.stream().filter(classes -> classes.contains(className)).count();
            assertEquals(1, owners, className + " belongs to " + owners + " gates");
        }
    }

    private static List<String> compiledPluginClasses() throws Exception {
        Path gate = Path.of(Objects.requireNonNull(ModPresence.class.getResource(SHIPPED_CLASS)).toURI())
                .getParent();
        Path root = classesRoot(gate);
        Set<String> classes = new TreeSet<>();
        for (Path config : configs()) {
            String plugin = read(config).get(PLUGIN_KEY).getAsString();
            Path pluginPackage = root.resolve(plugin.substring(0, plugin.lastIndexOf('.')).replace('.', '/'));
            try (Stream<Path> files = Files.walk(pluginPackage)) {
                files.map(file -> root.relativize(file).toString().replace('\\', '/'))
                        .filter(name -> name.endsWith(CLASS_SUFFIX))
                        .map(name -> name.substring(0, name.length() - CLASS_SUFFIX.length()))
                        .forEach(classes::add);
            }
        }

        return List.copyOf(classes);
    }

    private static Path classesRoot(Path gate) {
        int depth = Path.of(GATE_PACKAGE).getNameCount();
        return gate.getRoot().resolve(gate.subpath(0, gate.getNameCount() - depth));
    }

    private static List<String> indexedClasses() throws IOException {
        try (InputStream index = ModPresence.class.getClassLoader().getResourceAsStream(ModPresence.CLASS_INDEX)) {
            assertNotNull(index, ModPresence.CLASS_INDEX + " is not in common's main output");
            return new String(index.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .filter(line -> !line.isBlank())
                    .sorted()
                    .toList();
        }
    }

    private static ModPresenceGatePlugin pluginOf(JsonObject config) throws ReflectiveOperationException {
        return Class.forName(config.get(PLUGIN_KEY).getAsString()).asSubclass(ModPresenceGatePlugin.class)
                .getDeclaredConstructor().newInstance();
    }

    private static List<String> mixinsOf(JsonObject config) {
        List<String> mixins = new ArrayList<>();
        for (String key : MIXIN_LIST_KEYS) {
            if (config.get(key) instanceof JsonArray list) {
                for (JsonElement mixin : list) {
                    mixins.add(mixin.getAsString());
                }
            }
        }

        return mixins;
    }

    private static JsonObject read(Path config) throws IOException {
        try (Reader reader = Files.newBufferedReader(config)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
