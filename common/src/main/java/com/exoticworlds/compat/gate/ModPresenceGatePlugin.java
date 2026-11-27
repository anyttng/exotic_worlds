package com.exoticworlds.compat.gate;

import java.util.List;

import org.spongepowered.asm.mixin.MixinEnvironment;

import com.exoticworlds.MixinGatePlugin;

public abstract class ModPresenceGatePlugin extends MixinGatePlugin {
    private final List<ModPresence> gates;

    protected ModPresenceGatePlugin(ModPresence... gates) {
        this.gates = List.of(gates);
    }

    public List<ModPresence> getGates() {
        return this.gates;
    }

    @Override
    public void onLoad(String mixinPackage) {
        ModPresence.runningSide(MixinEnvironment.getCurrentEnvironment().getSide());
        this.gates.forEach(ModPresence::isAdmitted);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        for (ModPresence gate : this.gates) {
            if (gate.isCovering(mixinClassName)) {
                return gate.isAdmitting(mixinClassName);
            }
        }

        return false;
    }
}
