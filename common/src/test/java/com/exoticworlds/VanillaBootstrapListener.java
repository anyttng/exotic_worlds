package com.exoticworlds;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

import com.exoticworlds.platform.LoaderlessPlatform;
import com.exoticworlds.platform.Platforms;
import com.exoticworlds.shape.WorldOptionSetup;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

public final class VanillaBootstrapListener implements LauncherSessionListener {
    @Override
    public void launcherSessionOpened(LauncherSession session) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Platforms.set(new LoaderlessPlatform());
        WorldOptionSetup.registerAll(false);
    }
}
