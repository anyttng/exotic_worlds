package com.exoticworlds.compat.c2me;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.ModSymbol;

class C2meGateSymbolsTest {
    private static final ClassLoader LOADER = C2meGateSymbolsTest.class.getClassLoader();

    private static final String GAME_PACKAGE = "net/minecraft/";

    private static final List<ModSymbol> GATE_SYMBOLS = List.of(C2meChunkSystem.CHUNK_SYSTEM_SCHEDULING_MANAGER,
            C2meNoTickVd.NO_TICK_LOADER_VIEW_DISTANCE, C2meAquifer.SAMPLER_INIT_HANDLER);

    @Test
    void everyC2meGateNamesASymbolTheCompiledAgainstModulesCarry() {
        for (ModSymbol symbol : GATE_SYMBOLS) {
            assertTrue(symbol.carriedBy(LOADER),
                    symbol + " is gone from the C2ME modules this compat compiles against, so its gate would refuse "
                            + "a C2ME that works");
        }
    }

    @Test
    void noC2meGateNamesAGameClass() {
        for (ModSymbol symbol : GATE_SYMBOLS) {
            assertFalse(namesGameClass(symbol),
                    symbol + " names a game class, and nothing remaps a ModSymbol literal, so its gate reads false "
                            + "on every obfuscated game line");
        }
    }

    private static boolean namesGameClass(ModSymbol symbol) {
        return symbol.owner().contains(GAME_PACKAGE)
                || symbol.member().contains(GAME_PACKAGE)
                || symbol.descriptor().contains(GAME_PACKAGE);
    }
}
