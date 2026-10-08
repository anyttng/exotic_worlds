package com.exoticworlds.compat.betterend;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.ModSymbol;

class BetterEndGateSymbolTest {
    @Test
    void theGateNamesSymbolsTheCompiledAgainstBetterEndCarries() {
        for (ModSymbol symbol : BetterEndMixinPlugin.SYMBOLS) {
            assertTrue(symbol.carriedBy(BetterEndGateSymbolTest.class.getClassLoader()),
                    symbol + " is gone from the BetterEnd this compat compiles against, so its gate would refuse "
                            + "a BetterEnd that works");
        }
    }
}
