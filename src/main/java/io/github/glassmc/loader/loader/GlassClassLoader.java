package io.github.glassmc.loader.loader;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class GlassClassLoader extends URLClassLoader {

    private final ClassLoader parent = GlassClassLoader.class.getClassLoader();

    private final List<Object> transformers = new ArrayList<>();
    private final Method transformMethod;

    private final List<String> classesToReload = new ArrayList<>();
    private final Instrumentation instrumentation;

    private final Map<String, Class<?>> cache = new HashMap<>();

    public GlassClassLoader() throws ClassNotFoundException, NoSuchMethodException {
        super(getLoaderURLs(), null);
        transformMethod = this.loadClass("io.github.glassmc.loader.loader.ITransformer").getMethod("transform", String.class, byte[].class);

        Instrumentation instrumentation;
        try {
            instrumentation = ByteBuddyAgent.install();
        } catch(IllegalStateException e) {
            instrumentation = null;
        }
        this.instrumentation = instrumentation;
    }

    public void addURL(URL url) {
        super.addURL(url);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if(name.startsWith("java.") || name.startsWith("jdk.internal.") || name.startsWith("sun.")) {
            return this.parent.loadClass(name);
        }

        try {
            Method method = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            method.setAccessible(true);

            Class<?> clazz = (Class<?>) method.invoke(this.parent, name);
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
            } catch (IllegalAccessException | InvocationTargetException e) {
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

    private static URL[] getLoaderURLs() {
        List<URL> urls = new ArrayList<>();

        String baseClassPath = System.getProperty("java.class.path");
        for(String url : baseClassPath.split(File.pathSeparator)) {
            try {
                urls.add(new File(url).toURI().toURL());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        File shardDirectory = new File("shards");

        try {
            File[] files = shardDirectory.listFiles();
            if(files != null) {
                for (File file : files) {
                    urls.add(file.toURI().toURL());
                }
            }
        } catch(MalformedURLException e) {
            e.printStackTrace();
        }
        return urls.toArray(new URL[0]);
    }

    public void addTransformer(Object transformer) {
        this.transformers.add(transformer);
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
