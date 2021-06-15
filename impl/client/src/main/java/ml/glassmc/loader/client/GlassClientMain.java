package ml.glassmc.loader.client;

import ml.glassmc.loader.GlassLoader;
import ml.glassmc.loader.ShardSpecification;
import ml.glassmc.loader.client.hook.ClientPreInitializeHook;
import ml.glassmc.loader.launch.Launcher;

import java.util.Arrays;

public class GlassClientMain {

    public static void main(String[] args) {
        GlassLoader.getInstance().registerVirtualShard(new ShardSpecification("loader-client", "0.0.1"));
        GlassLoader.getInstance().registerVirtualShard(new ShardSpecification("client", args[Arrays.asList(args).indexOf("--version") + 1]));

        GlassLoader.getInstance().runHooks("client-initialize-pre");

        Launcher launcher = GlassLoader.getInstance().getLauncher();
        launcher.setTarget("ml.glassmc.loader.client.ClientWrapper");
        launcher.addArguments(args);
        launcher.launch();
    }

}
