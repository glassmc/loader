package io.github.glassmc.loader.client;

import io.github.glassmc.loader.GlassLoader;
import io.github.glassmc.loader.ShardSpecification;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class ClientWrapper {

    public static void main(String[] args) {
        GlassLoader.getInstance().registerVirtualShard(new ShardSpecification("client", args[Arrays.asList(args).indexOf("--version") + 1]));


    }

}
