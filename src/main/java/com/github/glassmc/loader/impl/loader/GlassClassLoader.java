package com.github.glassmc.loader.impl.loader;

import com.github.glassmc.loader.api.loader.TransformerOrder;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.Manifest;

public class GlassClassLoader extends URLClassLoader {

    private final List<Object>[] transformers = new List[3];
    private final Method canTransformMethod;
    private final Method transformMethod;
    private final Method acceptsBlankMethod;

    private final Object glassLoaderObject;
    private final Method filterClassesMethod;

    private final List<String> exclusions = new ArrayList<>();

    private final List<URL> urls = new ArrayList<>();

    private final ClassLoader parent = GlassClassLoader.class.getClassLoader();

    public GlassClassLoader() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        super(new URL[0], null);

        for (int i = 0; i < 3; i++) {
            this.transformers[i] = new ArrayList<>();
        }

        this.exclusions.add("java.");
        this.exclusions.add("jdk.");
        this.exclusions.add("javax.");

        this.exclusions.add("sun.");
        this.exclusions.add("com.sun.");
        this.exclusions.add("org.xml.");
        this.exclusions.add("org.w3c.");

        this.exclusions.add("org.apache.");
        this.exclusions.add("org.slf4j.");
        this.exclusions.add("com.mojang.blocklist.");

        this.exclusions.add("com.github.glassmc.loader.api.loader.Transformer");
        this.exclusions.add("com.github.glassmc.loader.api.ClassDefinition");

        this.canTransformMethod = this.loadClass("com.github.glassmc.loader.api.loader.Transformer").getMethod("canTransform", String.class);
        this.transformMethod = this.loadClass("com.github.glassmc.loader.api.loader.Transformer").getMethod("transform", String.class, byte[].class);
        this.acceptsBlankMethod = this.loadClass("com.github.glassmc.loader.api.loader.Transformer").getMethod("acceptsBlank");

        this.filterClassesMethod = this.loadClass("com.github.glassmc.loader.impl.GlassLoaderImpl").getMethod("filterClasses", String.class, String[].class, byte[][].class);
        this.glassLoaderObject = this.loadClass("com.github.glassmc.loader.api.GlassLoader").getMethod("getInstance").invoke(null);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        for (String exclusion : this.exclusions) {
            if (name.startsWith(exclusion)) {
                try {
                    return parent.loadClass(name);
                } catch (ClassNotFoundException ignored) {

                }
            }
        }

        Class<?> clazz = super.findLoadedClass(name);
        if(clazz == null) {
            byte[] data = this.getModifiedBytes(name);
            clazz = super.defineClass(name, data, 0, data.length);
        }
        return clazz;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz = this.loadClass(name);
        if (resolve) {
            this.resolveClass(clazz);
        }
        return clazz;
    }

    public byte[] getModifiedBytes(String name) throws ClassNotFoundException {
        byte[] data = loadClassData(name);

        for(Object transformer : this.getTransformers()) {
            String formattedName = name.replace(".", "/");
            try {
                if ((boolean) canTransformMethod.invoke(transformer, formattedName) && (data.length > 0 || (boolean) acceptsBlankMethod.invoke(transformer))) {
                    data = (byte[]) transformMethod.invoke(transformer, formattedName, data);
                }
            } catch(IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        if (data.length == 0) {
            throw new ClassNotFoundException(name);
        }

        return data;
    }

    private List<Object> getTransformers() {
        List<Object> transformers = new ArrayList<>();

        transformers.addAll(this.transformers[0]);
        transformers.addAll(this.transformers[1]);
        transformers.addAll(this.transformers[2]);

        return transformers;
    }

    private byte[] loadClassData(String className) {
        try {
            Enumeration<URL> resources = this.getResources(className.replace(".", "/") + ".class");

            List<String> locations = new ArrayList<>();
            List<byte[]> datas = new ArrayList<>();

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();

                locations.add(resource.toString());
                datas.add(IOUtils.toByteArray(resource));
            }

            byte[][] result;
            if (this.filterClassesMethod != null && this.glassLoaderObject != null && datas.size() > 0) {
                result = (byte[][]) this.filterClassesMethod.invoke(this.glassLoaderObject, className.replace(".", "/"), locations.toArray(new String[0]), datas.toArray(new byte[0][]));

                if (result.length == 0) {
                    result = new byte[][] { new byte[0] };
                }
            } else if (datas.size() > 0) {
                result = datas.toArray(new byte[0][]);
            } else {
                result = new byte[][] { new byte[0] };
            }

            return result[0];
        } catch (IOException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return new byte[0];
        }
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
    public void addTransformer(Class<?> transformer, TransformerOrder order) {
        try {
            int listIndex;
            switch (order) {
                case FIRST:
                    listIndex = 0;
                    break;
                case LAST:
                    listIndex = 2;
                    break;
                case DEFAULT:
                    listIndex = 1;
                    break;
                default:
                    listIndex = -1;
                    break;
            }

            this.transformers[listIndex].add(transformer.getConstructor().newInstance());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

}
