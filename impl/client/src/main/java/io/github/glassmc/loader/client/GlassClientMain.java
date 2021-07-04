package io.github.glassmc.loader.client;

import io.github.glassmc.loader.launch.GlassClassLoader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GlassClientMain {

    public static void main(String[] args) {
        ClassLoader classLoader = new GlassClassLoader();
        try {
            Class<?> wrapperClass = classLoader.loadClass("io.github.glassmc.loader.client.GlassClientLauncher");
            Method mainMethod = wrapperClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) args);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
