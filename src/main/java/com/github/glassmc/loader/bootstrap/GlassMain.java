package com.github.glassmc.loader.bootstrap;

import com.github.glassmc.loader.impl.loader.GlassClassLoader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GlassMain {

    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ClassLoader classLoader = new GlassClassLoader();
        try {
            Class<?> wrapperClass = classLoader.loadClass("com.github.glassmc.loader.bootstrap.GlassLauncher");
            Method mainMethod = wrapperClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) args);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
