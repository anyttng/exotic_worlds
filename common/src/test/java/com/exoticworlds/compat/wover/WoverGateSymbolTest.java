package com.exoticworlds.compat.wover;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.ModSymbol;

class WoverGateSymbolTest {
    @Test
    void theGateNamesSymbolsTheCompiledAgainstWorldWeaverCarries() {
        for (ModSymbol symbol : WoverMixinPlugin.SYMBOLS) {
            assertTrue(symbol.carriedBy(WoverGateSymbolTest.class.getClassLoader()),
                    symbol + " is gone from the WorldWeaver this compat compiles against, so its gate would refuse "
                            + "a WorldWeaver that works");
        }
    }
}
