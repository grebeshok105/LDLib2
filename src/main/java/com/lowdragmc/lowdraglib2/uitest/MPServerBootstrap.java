package com.lowdragmc.lowdraglib2.uitest;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.uitest.mp.MPRunConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * Arms {@link MPServerRunner} when this process is the dedicated server of a multi-process run —
 * that is, when the {@code runMpServer} Gradle run set the mptest role/hub system properties.
 * Inert everywhere else, including ordinary {@code runServer} launches.
 */
public final class MPServerBootstrap {

    @Nullable
    private static MPServerRunner runner;

    private MPServerBootstrap() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(MPServerBootstrap::onServerStarted);
        ServerTickEvents.END_SERVER_TICK.register(server -> onServerTick());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> onServerStopped());
    }

    public static void onServerStarted(MinecraftServer server) {
        if (!Platform.isDevEnv()) return;
        var config = MPRunConfig.fromSystemProperties();
        if (config == null || !config.isServer()) return;
        runner = new MPServerRunner(config, server);
        runner.start();
    }

    public static void onServerTick() {
        if (runner != null) {
            runner.tick();
        }
    }

    public static void onServerStopped() {
        if (runner != null) {
            runner.close();
            runner = null;
        }
    }
}
