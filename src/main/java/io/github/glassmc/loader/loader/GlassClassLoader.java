package io.github.glassmc.loader.loader;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class GlassClassLoader extends URLClassLoader {

    private static GlassClassLoader instance;

    public static GlassClassLoader getInstance() {
        return instance;
    }

    private final ClassLoader parent = GlassClassLoader.class.getClassLoader();
    private final List<ITransformer> transformers = new ArrayList<>();

    public GlassClassLoader() {
        super(getLoaderURLs(), null);
        instance = this;
    }

    public void addURL(URL url) {
        super.addURL(url);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if(name.startsWith("java.") || name.startsWith("jdk.") || name.startsWith("sun.") || name.startsWith("com.sun.") || name.startsWith("javax.") || name.startsWith("org.xml.") || name.startsWith("org.w3c.")) {
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

        byte[] data = loadClassData(name);
        if(data == null) {
            throw new ClassNotFoundException(name);
        }

        for(ITransformer transformer : this.transformers) {
            data = transformer.transform(name, data);
        }

        return defineClass(name, data, 0, data.length);
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

    public List<ITransformer> getTransformers() {
        return transformers;
    }

}
