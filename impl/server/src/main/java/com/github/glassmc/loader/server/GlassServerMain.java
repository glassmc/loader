package com.github.glassmc.loader.server;

import com.github.glassmc.loader.loader.GlassClassLoader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GlassServerMain {

    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException {
        ClassLoader classLoader = new GlassClassLoader();
        try {
            Class<?> wrapperClass = classLoader.loadClass("com.github.glassmc.loader.server.GlassServerLauncher");
            Method mainMethod = wrapperClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) args);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
