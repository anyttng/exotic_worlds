package com.exoticworlds.compat.simpleatlas;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.ModSymbol;

class SimpleAtlasGateSymbolTest {
    @Test
    void theGateNamesSymbolsTheCompiledAgainstSimpleAtlasCarries() {
        for (ModSymbol symbol : SimpleAtlasMixinPlugin.SYMBOLS) {
            assertTrue(symbol.carriedBy(SimpleAtlasGateSymbolTest.class.getClassLoader()),
                    symbol + " is gone from the Simple Atlas this compat compiles against, so its gate would refuse "
                            + "a Simple Atlas that works");
        }
    }
}
