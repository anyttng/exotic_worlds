package com.exoticworlds.compat.seedviewer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.ModSymbol;

class SeedViewerGateSymbolTest {
    @Test
    void theGateNamesSymbolsTheCompiledAgainstSeedViewerCarries() {
        for (ModSymbol symbol : SeedViewerMixinPlugin.SYMBOLS) {
            assertTrue(symbol.carriedBy(SeedViewerGateSymbolTest.class.getClassLoader()),
                    symbol + " is gone from the Seed Viewer this compat compiles against, so its gate would refuse "
                            + "a Seed Viewer that works");
        }
    }
}
