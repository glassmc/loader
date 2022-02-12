package com.github.glassmc.loader.impl.loader;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class GlassClassLoader extends URLClassLoader {

    private final List<Object> transformers = new ArrayList<>();
    private final Method canTransformMethod;
    private final Method transformMethod;

    private final List<String> exclusions = new ArrayList<>();

    private final List<URL> urls = new ArrayList<>();

    private final ClassLoader parent = GlassClassLoader.class.getClassLoader();

    public GlassClassLoader() throws ClassNotFoundException, NoSuchMethodException {
        super(new URL[0], null);
        //this.urls.addAll(Arrays.asList(super.getURLs()));

        this.exclusions.add("java.");
        this.exclusions.add("jdk.internal.");
        this.exclusions.add("javax.");

        this.exclusions.add("sun.");
        this.exclusions.add("com.sun.");
        this.exclusions.add("org.xml.");
        this.exclusions.add("org.w3c.");

        this.exclusions.add("org.apache.");
        this.exclusions.add("org.slf4j.");

        this.canTransformMethod = this.loadClass("com.github.glassmc.loader.api.loader.Transformer").getMethod("canTransform", String.class);
        this.transformMethod = this.loadClass("com.github.glassmc.loader.api.loader.Transformer").getMethod("transform", String.class, byte[].class);

    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        for (String exclusion : this.exclusions) {
            if (name.startsWith(exclusion)) {
                return parent.loadClass(name);
            }
        }

        Class<?> clazz = super.findLoadedClass(name);
        if(clazz == null) {
            byte[] data = this.getModifiedBytes(name);
            clazz = super.defineClass(name, data, 0, data.length);
        }
        return clazz;
    }

    public byte[] getModifiedBytes(String name) throws ClassNotFoundException {
        byte[] data = loadClassData(name);
        if(data == null) {
            throw new ClassNotFoundException(name);
        }

        for(Object transformer : this.transformers) {
            String formattedName = name.replace(".", "/");
            try {
                if ((boolean) canTransformMethod.invoke(transformer, formattedName)) {
                    data = (byte[]) transformMethod.invoke(transformer, formattedName, data);
                }
            } catch(IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        return data;
    }

    private byte[] loadClassData(String className) {
        InputStream inputStream = this.getResourceAsStream(className.replace(".", "/") + ".class");
        try {
            if (inputStream != null) {
                return IOUtils.toByteArray(inputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> parentResources = Collections.list(parent.getResources(name));

        List<URL> filteredURLs = new ArrayList<>(parentResources);

        for (URL pathUrl : Collections.list(this.findResources(name))) {
            for (URL url : this.urls) {
                if (pathUrl.getFile().contains(url.getFile())) {
                    filteredURLs.add(pathUrl);
                }
            }
        }

        return Collections.enumeration(filteredURLs);
    }

    @Override
    public URL getResource(String name) {
        try {
            Enumeration<URL> resources = this.getResources(name);
            if(resources.hasMoreElements()) {
                return resources.nextElement();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        return parent.getResource(name);
    }

    public void addURL(URL url) {
        this.urls.add(url);
        super.addURL(url);
    }

    @SuppressWarnings("unused")
    public void removeURL(URL url) {
        this.urls.remove(url);
    }

    @SuppressWarnings("unused")
    public void addTransformer(Class<?> transformer) {
        try {
            this.transformers.add(transformer.getConstructor().newInstance());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

}
