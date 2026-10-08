package com.exoticworlds.compat.lithium;

import java.lang.reflect.Field;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;

final class LithiumOptionGate {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String MIXIN_ROOT = "net.caffeinemc.mods.lithium.mixin.";
    private static final String MIXIN_PLUGIN_CLASS = MIXIN_ROOT + "LithiumMixinPlugin";
    private static final String DISABLE_ALL_FIELD = "DISABLE_ALL_MIXINS";
    private static final String CONFIG_FIELD = "CONFIG";
    private static final String EFFECTIVE_OPTION_METHOD = "getEffectiveOptionForMixin";
    private static final String IS_ENABLED_METHOD = "isEnabled";

    private final String label;
    private final String mixinRule;
    private final ModPresence gate;

    LithiumOptionGate(String label, String mixinClass, ModPresence gate) {
        this.label = label;
        this.mixinRule = mixinClass.substring(MIXIN_ROOT.length());
        this.gate = gate;
    }

    boolean read(ClassLoader classLoader) {
        if (!this.gate.present()) {
            return false;
        }

        try {
            boolean enabled = optionEnabled(classLoader);
            LOGGER.info("{} option_enabled={}", this.label, enabled);
            return enabled;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unreadable) {
            // A read failure applies the mixin: an absent target then fails its apply in the log, while standing down leaves the seam blind and silent.
            LOGGER.warn("{} option_enabled=unreadable, applying", this.label, unreadable);
            return true;
        }
    }

    private boolean optionEnabled(ClassLoader classLoader) throws ReflectiveOperationException {
        Class<?> plugin = Class.forName(MIXIN_PLUGIN_CLASS, true, classLoader);
        if (plugin.getField(DISABLE_ALL_FIELD).getBoolean(null)) {
            return false;
        }

        Field configField = plugin.getDeclaredField(CONFIG_FIELD);
        configField.setAccessible(true);
        Object config = configField.get(null);
        if (config == null) {
            throw new IllegalStateException(MIXIN_PLUGIN_CLASS + "." + CONFIG_FIELD + " is not loaded yet");
        }

        Object option = config.getClass().getMethod(EFFECTIVE_OPTION_METHOD, String.class).invoke(config, this.mixinRule);
        return option != null && (Boolean) option.getClass().getMethod(IS_ENABLED_METHOD).invoke(option);
    }
}
