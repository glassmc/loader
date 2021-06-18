package io.github.glassmc.loader.launch;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Wrapper {

    public static void launch(String target, String[] args) {
        try {
            Class<?> targetClass = Class.forName(target);
            Method targetMethod = targetClass.getMethod("main", String[].class);
            targetMethod.invoke(null, (Object) args);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
