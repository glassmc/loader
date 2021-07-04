package io.github.glassmc.loader.client;

import io.github.glassmc.loader.GlassLoader;
import io.github.glassmc.loader.ShardSpecification;
import io.github.glassmc.loader.launch.Launcher;

import java.util.Arrays;

public class GlassClientLauncher {

    public static void main(String[] args) {
        GlassLoader.getInstance().registerVirtualShard(new ShardSpecification("client", args[Arrays.asList(args).indexOf("--version") + 1]));

        GlassLoader.getInstance().runHooks("client-initialize-pre");

        Launcher launcher = GlassLoader.getInstance().getLauncher();
        launcher.setTarget("io.github.glassmc.loader.client.ClientWrapper");
        launcher.addArguments(args);
        launcher.launch();
    }

}
