package com.github.glassmc.loader.server;

import com.github.glassmc.loader.api.GlassLoader;
import com.github.glassmc.loader.impl.GlassLoaderImpl;
import com.github.glassmc.loader.impl.ShardSpecification;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class GlassServerLauncher {

    public static void main(String[] args) {
        GlassLoaderImpl glassLoader = (GlassLoaderImpl) GlassLoader.getInstance();
        glassLoader.registerVirtualShard(new ShardSpecification("server", args[Arrays.asList(args).indexOf("--version") + 1]));

        glassLoader.loadUpdateShards();

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
