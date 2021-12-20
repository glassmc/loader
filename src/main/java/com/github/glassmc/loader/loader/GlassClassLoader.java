package com.github.glassmc.loader.loader;

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

    private final List<URL> urls = new ArrayList<>();

    public GlassClassLoader() throws ClassNotFoundException, NoSuchMethodException {
        super(new URL[0], GlassClassLoader.class.getClassLoader());
        this.urls.addAll(Arrays.asList(super.getURLs()));
        this.canTransformMethod = this.loadClass("com.github.glassmc.loader.loader.ITransformer").getMethod("canTransform", String.class);
        this.transformMethod = this.loadClass("com.github.glassmc.loader.loader.ITransformer").getMethod("transform", String.class, byte[].class);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if(name.startsWith("java.") || name.startsWith("jdk.internal.") || name.startsWith("sun.") || name.startsWith("com.sun.") || name.startsWith("javax.") || name.startsWith("org.xml.") || name.startsWith("org.w3c.")) {
            return super.loadClass(name, resolve);
        }

        Class<?> clazz = this.findLoadedClass(name);
        if(clazz == null) {
            byte[] data = this.getModifiedBytes(name);
            clazz = defineClass(name, data, 0, data.length);
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
        List<URL> parentResources = Collections.list(this.getParent().getResources(name));
        List<URL> filteredURLs = new ArrayList<>();
        Enumeration<URL> urls = super.getResources(name);
        while(urls.hasMoreElements()) {
            URL url = urls.nextElement();
            for(URL pathURL : this.urls) {
                if(url.getFile().contains(pathURL.getFile())) {
                    filteredURLs.add(url);
                }
            }

            if(parentResources.contains(url)) {
                filteredURLs.add(url);
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
        return super.getResource(name);
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
