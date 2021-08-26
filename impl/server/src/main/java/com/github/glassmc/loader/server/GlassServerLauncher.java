package com.github.glassmc.loader.server;

import com.github.glassmc.loader.GlassLoader;
import com.github.glassmc.loader.ShardSpecification;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class GlassServerLauncher {

    public static void main(String[] args) {
        GlassLoader.getInstance().setProgramArguments(args);
        GlassLoader.getInstance().registerVirtualShard(new ShardSpecification("server", args[Arrays.asList(args).indexOf("--version") + 1]));
        GlassLoader.getInstance().appendExternalShards();

        GlassLoader.getInstance().loadUpdateShards();

        try {
            Class<?> mainClass;
            try {
                mainClass = Class.forName("net.minecraft.server.Main");
            } catch (ClassNotFoundException e) {
                mainClass = Class.forName("net.minecraft.server.MinecraftServer");
            }
            Method mainMethod = mainClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) args);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
