package com.exoticworlds.compat;

import java.util.List;

import com.exoticworlds.MixinGatePlugin;

public abstract class ModPresenceGatePlugin extends MixinGatePlugin {
    private final List<ModPresence> gates;

    protected ModPresenceGatePlugin(ModPresence... gates) {
        this.gates = List.of(gates);
    }

    public List<ModPresence> gates() {
        return this.gates;
    }

    @Override
    public void onLoad(String mixinPackage) {
        this.gates.forEach(ModPresence::present);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        for (ModPresence gate : this.gates) {
            if (gate.covers(mixinClassName)) {
                return gate.admits(mixinClassName);
            }
        }

        return false;
    }
}
