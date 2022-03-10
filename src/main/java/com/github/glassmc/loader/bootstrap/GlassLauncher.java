package com.github.glassmc.loader.bootstrap;

import com.github.glassmc.loader.api.GlassLoader;
import com.github.glassmc.loader.impl.GlassLoaderImpl;
import com.github.glassmc.loader.impl.ShardSpecification;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class GlassLauncher {

    public static void main(String[] args) {
        String environment = args[Arrays.asList(args).indexOf("--environment") + 1];
        GlassLoaderImpl glassLoader = (GlassLoaderImpl) GlassLoader.getInstance();
        glassLoader.registerVirtualShard(new ShardSpecification(environment, args[Arrays.asList(args).indexOf("--version") + 1]));
        glassLoader.preLoad();

        glassLoader.loadUpdateShards();

        try {
            Class<?> mainClass;

            if (environment.equals("client")) {
                mainClass = Class.forName("net.minecraft.client.main.Main");
            } else {
                try {
                    mainClass = Class.forName("net.minecraft.server.Main");
                } catch (ClassNotFoundException e) {
                    mainClass = Class.forName("net.minecraft.server.MinecraftServer");
                }
            }

            Method mainMethod = mainClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) args);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
