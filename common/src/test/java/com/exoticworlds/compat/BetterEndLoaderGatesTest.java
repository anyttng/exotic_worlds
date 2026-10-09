package com.exoticworlds.compat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

class BetterEndLoaderGatesTest {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String REPO_ROOT_PROPERTY = "toroidal.repoRoot";
    private static final String NEOFORGE_JAR_PROPERTY = "toroidal.betterEndNeoForgeJar";
    private static final String LOADER_RESOURCES = "src/main/resources";
    private static final String TERRAIN_GENERATOR = "org/betterx/betterend/world/generator/TerrainGenerator.class";
    private static final String SHARED_CONFIG = "exotic_worlds.compat.betterend.mixins.json";
    private static final String NEOFORGE = "neoforge";
    private static final String FABRIC = "fabric";

    @Test
    void theSharedConfigHoldsAgainstBothBuilds() throws IOException {
        assertTrue(admitted(SHARED_CONFIG, FABRIC, null), "the shared config refuses the Fabric BetterEnd");
        assertTrue(admitted(SHARED_CONFIG, NEOFORGE, neoForgeJar()), "the shared config refuses the NeoForge BetterEnd");
    }

    @Test
    void eachLoaderConfigHoldsAgainstItsOwnBuild() throws IOException {
        assertTrue(admitted(loaderConfig(FABRIC), FABRIC, null), "the Fabric config refuses the Fabric BetterEnd");
        assertTrue(admitted(loaderConfig(NEOFORGE), NEOFORGE, neoForgeJar()),
                "the NeoForge config refuses the NeoForge BetterEnd");
    }

    @Test
    void eachLoaderConfigIsRefusedByTheOtherBuild() throws IOException {
        assertFalse(admitted(loaderConfig(FABRIC), FABRIC, neoForgeJar()),
                "the Fabric overloads hold on the NeoForge BetterEnd, so the split is not needed");
        assertFalse(admitted(loaderConfig(NEOFORGE), NEOFORGE, null),
                "the NeoForge overloads hold on the Fabric BetterEnd, so the split is not needed");
    }

    private static boolean admitted(String config, String loader, @Nullable Path betterEndJar) throws IOException {
        List<URL> first = new ArrayList<>();
        if (betterEndJar != null) {
            first.add(betterEndJar.toUri().toURL());
        }

        first.add(Path.of(System.getProperty(REPO_ROOT_PROPERTY)).resolve(loader).resolve(LOADER_RESOURCES).toUri()
                .toURL());
        try (URLClassLoader overrides = new URLClassLoader(first.toArray(URL[]::new), null)) {
            ClassLoader build = new ClassLoader(BetterEndLoaderGatesTest.class.getClassLoader()) {
                @Override
                public URL getResource(String name) {
                    URL found = overrides.findResource(name);
                    return found != null ? found : super.getResource(name);
                }
            };
            return ModPresence.gate(LOGGER, "[betterend-compat] gate betterend_" + loader + "_present")
                    .probing(TERRAIN_GENERATOR)
                    .checking(config)
                    .build(build)
                    .present();
        }
    }

    private static String loaderConfig(String loader) {
        return "exotic_worlds.compat.betterend." + loader + ".mixins.json";
    }

    private static Path neoForgeJar() {
        Path jar = Path.of(System.getProperty(NEOFORGE_JAR_PROPERTY, ""));
        assertTrue(Files.isRegularFile(jar), "No NeoForge BetterEnd jar at '" + jar + "'");
        return jar;
    }
}
