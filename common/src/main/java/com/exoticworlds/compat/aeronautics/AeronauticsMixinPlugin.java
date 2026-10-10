package com.exoticworlds.compat.aeronautics;

import java.util.List;

import com.exoticworlds.compat.ModPresenceGatePlugin;

public class AeronauticsMixinPlugin extends ModPresenceGatePlugin {
    static final String CONFIG = "exotic_worlds.compat.aeronautics.mixins.json";
    static final List<String> AERONAUTICS_MIXINS = List.of("PropellerActorBehaviourMixin");
    static final List<String> OFFROAD_MIXINS = List.of("MultiMiningSyncAccessor");

    public AeronauticsMixinPlugin() {
        super(AeronauticsMod.GATE, SimulatedMod.GATE, OffroadMod.GATE);
    }
}
