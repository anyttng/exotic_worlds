package com.exoticworlds.api.v1.client;

import java.util.Collection;
import java.util.List;

import com.exoticworlds.api.v1.option.WorldOption;
import com.exoticworlds.api.v1.option.WorldOptions;
import com.exoticworlds.bridge.v1.client.OptionControlsBridge;

/**
 * The client half of the world-option framework: which control an option is edited through, and the building of them
 * all for one screen. A declaration registers its factory here from the client side alone, beside the option itself.
 *
 * @deprecated no screen builds these controls any more, so a control registered for an option is never shown.
 *     Declare how the option is offered and drawn with a {@link com.exoticworlds.api.v2.option.DimensionOption};
 *     the World Shape Settings screen draws it.
 */
@Deprecated
public final class WorldOptionControls {

    /** Builds the control for one option on one screen. */
    @FunctionalInterface
    public interface Factory {
        WorldOptionControl create(WorldOptionContext context);
    }

    /**
     * Registers the control {@code option} is edited through.
     *
     * @throws IllegalStateException if the registration boundary has already closed, or a control is already
     *         registered under the key of {@code option}
     */
    public static void register(WorldOption<?> option, Factory factory) {
        OptionControlsBridge.register(option, factory);
    }

    /**
     * A control for every registered option that has one, in the order {@link WorldOptions#all} states — what a
     * shape's settings screen lays out. An option with no control registered is skipped.
     */
    public static List<WorldOptionControl> createAll(WorldOptionContext context) {
        return createAll(context, WorldOptions.all());
    }

    /**
     * A control for every option in {@code offered} that has one, still in the order {@link WorldOptions#all} states
     * — what the settings screen of a shape offering only some of the registered options lays out.
     */
    public static List<WorldOptionControl> createAll(WorldOptionContext context,
            Collection<? extends WorldOption<?>> offered) {
        return OptionControlsBridge.createAll(context, offered);
    }

    private WorldOptionControls() {
    }
}
