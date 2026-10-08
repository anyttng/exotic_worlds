package com.exoticworlds.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

class ModPresenceTest {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String SHIPPED_CLASS = "com/exoticworlds/compat/ModPresence.class";
    private static final String ABSENT_CLASS = "com/exoticworlds/compat/NoSuchModEntryPoint.class";
    private static final String CONFIG = "fixture.mixins.json";
    private static final String ABSENT_CONFIG = "no_such.mixins.json";
    private static final String FIXTURES = "com.exoticworlds.compat.fixture.MixinFixtures$";
    private static final String MIXIN_PREFIX = "MixinFixtures$";
    private static final String WRAP_PRESENT = "WrapOperationPresent";
    private static final String SHADOW_MISSING = "ShadowFieldMissing";
    private static final String INJECT_PRESENT = "InjectPresent";

    @Test
    void aGateWhoseCoveredMixinsAllHoldOpens() {
        assertTrue(ModPresence.gate(LOGGER, "[test-compat] gate holding_present")
                .probing(SHIPPED_CLASS)
                .checking(CONFIG, mixin -> !mixin.equals(MIXIN_PREFIX + SHADOW_MISSING))
                .build().present(), "every mixin the gate covers names only members its target has");
    }

    @Test
    void aGateClosesOnTheOneMixinOfSeveralThatLostAMember() {
        assertFalse(ModPresence.gate(LOGGER, "[test-compat] gate one_moved_present")
                .probing(SHIPPED_CLASS)
                .checking(CONFIG)
                .build().present(), "one covered mixin shadows a field its target no longer declares");
    }

    @Test
    void aMixinTheGateDoesNotCoverNeverClosesIt() {
        assertTrue(ModPresence.gate(LOGGER, "[test-compat] gate narrowed_present")
                .probing(SHIPPED_CLASS)
                .checking(CONFIG, (MIXIN_PREFIX + WRAP_PRESENT)::equals)
                .build().present(), "the refused mixin belongs to another gate's set");
    }

    @Test
    void aGateWhoseModIsAbsentClosesWithoutReadingItsConfig() {
        assertFalse(ModPresence.gate(LOGGER, "[test-compat] gate uninstalled_present")
                .probing(ABSENT_CLASS)
                .checking(ABSENT_CONFIG)
                .build().present(), "an absent mod closes the gate before its config is opened");
    }

    @Test
    void aGateClosesWhenAnyOfItsResourcesIsAbsent() {
        assertFalse(ModPresence.gate(LOGGER, "[test-compat] gate half_installed_present")
                .probing(SHIPPED_CLASS, ABSENT_CLASS)
                .build().present(), "a gate standing on two mods needs both");
    }

    @Test
    void aGateWithoutAConfigIsTheBareProbe() {
        assertTrue(ModPresence.gate(LOGGER, "[test-compat] gate shipped_present").probing(SHIPPED_CLASS).build()
                .present(), "the mod's own class file resolves through the loader the builder picks");
        assertFalse(ModPresence.gate(LOGGER, "[test-compat] gate absent_present").probing(ABSENT_CLASS).build()
                .present(), "no jar on the classpath carries that class file");
    }

    @Test
    void aGateCoversTheConfigMixinsItsPredicateTakesOnBothSides() {
        ModPresence gate = ModPresence.gate(LOGGER, "[test-compat] gate covering_present")
                .probing(SHIPPED_CLASS)
                .checking(CONFIG, mixin -> !mixin.equals(MIXIN_PREFIX + SHADOW_MISSING))
                .build();

        assertTrue(gate.covers(FIXTURES + WRAP_PRESENT), "a common mixin the predicate takes");
        assertTrue(gate.covers(FIXTURES + INJECT_PRESENT), "a client mixin the predicate takes");
        assertFalse(gate.covers(FIXTURES + SHADOW_MISSING), "the predicate leaves this one to another gate");
        assertFalse(gate.covers("com.exoticworlds.mixin." + WRAP_PRESENT), "a mixin of another config's package");
    }

    @Test
    void theBareProbeAnswersTheSameAsAGate() {
        assertTrue(ModPresence.probe(SHIPPED_CLASS), "the bare probe resolves the shipped class through the same loader");
        assertFalse(ModPresence.probe(ABSENT_CLASS), "the bare probe reads an absent class as absent");
    }

    @Test
    void theProbeAsksTheClassLoaderOnce() {
        CountingLoader loader = new CountingLoader(SHIPPED_CLASS);
        ModPresence gate = ModPresence.gate(LOGGER, "[test-compat] gate once_present").probing(SHIPPED_CLASS)
                .build(loader);

        assertTrue(gate.present());
        assertTrue(gate.present());

        assertEquals(1, loader.lookups, "the second present() answers from the value the first one probed");
        assertEquals(SHIPPED_CLASS, loader.asked, "the probe asks for the resource its gate names");
    }

    private static final class CountingLoader extends ClassLoader {
        private final String resolvable;

        private int lookups;
        private String asked;

        private CountingLoader(String resolvable) {
            super(null);
            this.resolvable = resolvable;
        }

        @Override
        public URL getResource(String name) {
            this.lookups++;
            this.asked = name;

            return name.equals(this.resolvable) ? ModPresenceTest.class.getResource("ModPresenceTest.class") : null;
        }
    }
}
