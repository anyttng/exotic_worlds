package com.exoticworlds.compat.vista;

public final class VistaPackets {
    public static void register() {
        if (VistaMixinPlugin.VISTA.admitted()) {
            ZonePacketRewriter.register();
        }
    }

    private VistaPackets() {
    }
}
