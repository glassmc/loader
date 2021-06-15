package ml.glassmc.loader.client;

import ml.glassmc.loader.GlassLoader;
import ml.glassmc.loader.ShardSpecification;
import ml.glassmc.loader.client.hook.ClientInitializeHook;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class ClientWrapper {

    public static void main(String[] args) {
        GlassLoader.getInstance().registerVirtualShard(new ShardSpecification("loader-client", "0.0.1"));
        GlassLoader.getInstance().registerVirtualShard(new ShardSpecification("client", args[Arrays.asList(args).indexOf("--version") + 1]));

        GlassLoader.getInstance().runHooks("client-initialize");

        try {
            Class<?> mainClass = Class.forName("net.minecraft.client.main.Main");
            Method mainMethod = mainClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) args);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
