package com.github.glassmc.loader.client;

import com.github.glassmc.loader.api.GlassLoader;
import com.github.glassmc.loader.impl.GlassLoaderImpl;
import com.github.glassmc.loader.impl.ShardSpecification;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class GlassClientLauncher {

    public static void main(String[] args) {
        GlassLoaderImpl glassLoader = (GlassLoaderImpl) GlassLoader.getInstance();
        glassLoader.registerVirtualShard(new ShardSpecification("client", args[Arrays.asList(args).indexOf("--version") + 1]));
        glassLoader.preLoad();

        glassLoader.loadUpdateShards();

        try {
            Class<?> mainClass = Class.forName("net.minecraft.client.main.Main");
            Method mainMethod = mainClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) args);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
