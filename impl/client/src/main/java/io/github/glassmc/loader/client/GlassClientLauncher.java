package io.github.glassmc.loader.client;

import io.github.glassmc.loader.GlassLoader;
import io.github.glassmc.loader.ShardSpecification;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class GlassClientLauncher {

    public static void main(String[] args) {
        GlassLoader.getInstance().registerVirtualShard(new ShardSpecification("client", args[Arrays.asList(args).indexOf("--version") + 1]));
        GlassLoader.getInstance().appendExternalShards();

        GlassLoader.getInstance().loadShards();
        GlassLoader.getInstance().loadShards();

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
