package com.github.glassmc.loader.loader;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class GlassClassLoader extends URLClassLoader {

    private final List<Object> transformers = new ArrayList<>();
    private final Method transformMethod;

    private final List<String> classesToReload = new ArrayList<>();
    private final Instrumentation instrumentation;

    private final Map<String, Class<?>> cache = new HashMap<>();

    private final List<URL> urls = new ArrayList<>();

    public GlassClassLoader() throws ClassNotFoundException, NoSuchMethodException {
        super(new URL[0], GlassClassLoader.class.getClassLoader());
        this.urls.addAll(Arrays.asList(super.getURLs()));
        this.transformMethod = this.loadClass("com.github.glassmc.loader.loader.ITransformer").getMethod("transform", String.class, byte[].class);

        Instrumentation instrumentation;
        try {
            instrumentation = ByteBuddyAgent.install();
        } catch(IllegalStateException e) {
            instrumentation = null;
        }
        this.instrumentation = instrumentation;
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if(name.startsWith("java.") || name.startsWith("jdk.internal.") || name.startsWith("sun.")) {
            return this.getParent().loadClass(name);
        }

        try {
            Method method = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            method.setAccessible(true);

            Class<?> clazz = (Class<?>) method.invoke(this.getParent(), name);
            if(clazz != null) {
                return clazz;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        Class<?> clazz = this.cache.get(name);
        if(clazz == null) {
            byte[] data = this.getModifiedBytes(name);
            clazz = defineClass(name, data, 0, data.length);
            this.cache.put(name, clazz);
        }
        return clazz;
    }

    public byte[] getModifiedBytes(String name) throws ClassNotFoundException {
        byte[] data = loadClassData(name);
        if(data == null) {
            throw new ClassNotFoundException(name);
        }

        for(Object transformer : this.transformers) {
            try {
                data = (byte[]) transformMethod.invoke(transformer, name, data);
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

    public void removeURL(URL url) {
        this.urls.remove(url);
    }

    public void addTransformer(Class<?> transformer) {
        try {
            this.transformers.add(transformer.getConstructor().newInstance());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public void removeTransformer(Class<?> transformerClass) {
        this.transformers.removeIf(transformer -> transformer.getClass().equals(transformerClass));
    }

    public void addReloadClass(String className) {
        this.classesToReload.add(className);
    }

    public void reloadClasses() throws UnsupportedOperationException {
        if(this.instrumentation != null) {
            ClassDefinition[] definitions = new ClassDefinition[this.classesToReload.size()];
            for(int i = 0; i < this.classesToReload.size(); i++) {
                try {
                    Class<?> clazz = this.loadClass(this.classesToReload.get(i));
                    definitions[i] = new ClassDefinition(clazz, this.getModifiedBytes(clazz.getName()));
                } catch(ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            try {
                instrumentation.redefineClasses(definitions);
            } catch(ClassNotFoundException | UnmodifiableClassException e) {
                e.printStackTrace();
            }
        }
        this.classesToReload.clear();
        if(this.instrumentation == null) {
            throw new UnsupportedOperationException("Agent not supported in currently environment!");
        }
    }

}