package com.toroidalworld.compat.distanthorizons;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.toroidalworld.compat.ModSymbol;

class DhGateSymbolTest {
    @Test
    void theGateNamesSymbolsTheCompiledAgainstDistantHorizonsCarries() {
        for (ModSymbol symbol : List.of(DhMixinPlugin.LEVEL_CHUNK_HASH_REPO, DhMixinPlugin.REPO_UPSERT_STATEMENT)) {
            assertTrue(symbol.carriedBy(DhGateSymbolTest.class.getClassLoader()),
                    symbol + " is gone from the Distant Horizons this compat compiles "
                            + "against, so its gate would refuse a Distant Horizons that works");
        }
    }
}
