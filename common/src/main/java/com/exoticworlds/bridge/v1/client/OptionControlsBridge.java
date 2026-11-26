package com.exoticworlds.bridge.v1.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.exoticworlds.api.v1.client.WorldOptionContext;
import com.exoticworlds.api.v1.client.WorldOptionControl;
import com.exoticworlds.api.v1.client.WorldOptionControls;
import com.exoticworlds.api.v1.option.WorldOption;
import com.exoticworlds.api.v1.option.WorldOptions;
import com.exoticworlds.core.registry.StartupRegistry;

public final class OptionControlsBridge {
    private static final StartupRegistry<String, WorldOptionControls.Factory> FACTORIES =
            new StartupRegistry<>("World option controls");

    public static void register(WorldOption<?> option, WorldOptionControls.Factory factory) {
        FACTORIES.register(option.key(), factory);
    }

    public static List<WorldOptionControl> createAll(WorldOptionContext context,
            Collection<? extends WorldOption<?>> offered) {
        Map<String, WorldOptionControls.Factory> factories = FACTORIES.getEntries();
        List<WorldOptionControl> controls = new ArrayList<>();
        for (WorldOption<?> option : WorldOptions.all()) {
            WorldOptionControls.Factory factory = factories.get(option.key());
            if (factory != null && offered.contains(option)) {
                controls.add(factory.create(context));
            }
        }

        return List.copyOf(controls);
    }

    private OptionControlsBridge() {
    }
}
