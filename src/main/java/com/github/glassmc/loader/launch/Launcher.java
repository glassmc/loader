package com.github.glassmc.loader.launch;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Launcher {

    private String target = null;
    private List<String> arguments = new ArrayList<>();
    private List<ITransformer> transformers = new ArrayList<>();

    public void launch() {
        ClassLoader classLoader = new Loader(transformers);

        try {
            Class<?> wrapperClass = classLoader.loadClass("com.github.glassmc.loader.launch.Wrapper");
            Method launchMethod = wrapperClass.getMethod("launch", String.class, String[].class);
            launchMethod.invoke(null, target, arguments.toArray(new String[0]));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void addArguments(String... arguments) {
        this.arguments.addAll(Arrays.asList(arguments));
    }

}
