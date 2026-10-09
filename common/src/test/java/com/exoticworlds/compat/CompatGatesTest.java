package com.exoticworlds.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
    // A mod published for Fabric alone ships intermediary bodies, which this Mojmap runtime cannot match by name.
    private static final Set<String> FABRIC_ONLY_CONFIGS = Set.of("exotic_worlds.compat.ftbxaerocompat.mixins.json");
    // No ScalableLux build for 1.21.1 carries the light engine interface the lock-release mixin names.
    private static final Set<String> SHUT_ON_THIS_LINE = Set.of("exotic_worlds.compat.scalablelux.mixins.json");

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
            long gates = plugin.gates().stream().filter(gate -> gate.covers(mixinClass)).count();
            assertEquals(1, gates, config.getFileName() + ": " + mixin + " is covered by " + gates + " gates");
        }
    }

    @ParameterizedTest
    @MethodSource("configs")
    void everyGateAdmitsTheModBuildTheSuiteCarries(Path config) throws Exception {
        assumeFalse(FABRIC_ONLY_CONFIGS.contains(config.getFileName().toString()),
                config.getFileName() + ": the mod has no Mojmap build for this runtime to read");
        if (SHUT_ON_THIS_LINE.contains(config.getFileName().toString())) {
            assertTrue(pluginOf(read(config)).gates().stream().noneMatch(ModPresence::present),
                    config.getFileName() + ": a gate shut on this line admits now; drop it from SHUT_ON_THIS_LINE");
            return;
        }

        for (ModPresence gate : pluginOf(read(config)).gates()) {
            assertTrue(gate.present(), config.getFileName() + ": a gate refuses the build this suite runs against; "
                    + "the refusal line in the test log names the mixin and the member");
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
