package com.exoticworlds.compat.lithium;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LithiumOptionGateTest {
    private static final String RULE = "world.game_events.dispatch.GameEventDispatcherMixin";

    @Test
    void absentSwitchReadsAnEnabledOption() throws Exception {
        assertTrue(LithiumOptionGate.optionEnabled(PluginWithoutSwitch.class, RULE));
    }

    @Test
    void absentSwitchReadsADisabledOption() throws Exception {
        assertFalse(LithiumOptionGate.optionEnabled(PluginWithoutSwitchOptionOff.class, RULE));
    }

    @Test
    void setSwitchStandsDownBeforeTheConfigLoads() throws Exception {
        assertFalse(LithiumOptionGate.optionEnabled(PluginSwitchedOff.class, RULE));
    }

    @Test
    void clearSwitchReadsTheOption() throws Exception {
        assertTrue(LithiumOptionGate.optionEnabled(PluginSwitchClear.class, RULE));
    }

    static final class PluginWithoutSwitch {
        static final StubConfig CONFIG = new StubConfig(RULE, true);
    }

    static final class PluginWithoutSwitchOptionOff {
        static final StubConfig CONFIG = new StubConfig(RULE, false);
    }

    static final class PluginSwitchedOff {
        public static final boolean DISABLE_ALL_MIXINS = true;
        static final StubConfig CONFIG = null;
    }

    static final class PluginSwitchClear {
        public static final boolean DISABLE_ALL_MIXINS = false;
        static final StubConfig CONFIG = new StubConfig(RULE, true);
    }

    static final class StubConfig {
        private final String rule;
        private final boolean enabled;

        StubConfig(String rule, boolean enabled) {
            this.rule = rule;
            this.enabled = enabled;
        }

        public StubOption getEffectiveOptionForMixin(String mixinRule) {
            return this.rule.equals(mixinRule) ? new StubOption(this.enabled) : null;
        }
    }

    static final class StubOption {
        private final boolean enabled;

        StubOption(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return this.enabled;
        }
    }
}
