package com.exoticworlds.compat.betterend;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.ModSymbol;

class BetterEndGateSymbolTest {
    private static final String NEOFORGE_JAR_PROPERTY = "toroidal.betterEndNeoForgeJar";

    @Test
    void theGateNamesSymbolsTheCompiledAgainstBetterEndCarries() {
        assertCarried(BetterEndGateSymbolTest.class.getClassLoader(), "the BetterEnd this compat compiles against");
    }

    @Test
    void theGateNamesSymbolsTheNeoForgeBetterEndCarries() throws IOException {
        Path jar = Path.of(System.getProperty(NEOFORGE_JAR_PROPERTY, ""));
        assertTrue(Files.isRegularFile(jar), "No NeoForge BetterEnd jar at '" + jar + "'");
        try (URLClassLoader neoForge = new URLClassLoader(new URL[] {jar.toUri().toURL()}, null)) {
            assertCarried(neoForge, "the NeoForge BetterEnd of this line");
        }
    }

    private static void assertCarried(ClassLoader build, String label) {
        for (ModSymbol symbol : BetterEndMixinPlugin.SYMBOLS) {
            assertTrue(symbol.carriedBy(build),
                    symbol + " is gone from " + label + ", so its gate would refuse a BetterEnd that works");
        }
    }
}
