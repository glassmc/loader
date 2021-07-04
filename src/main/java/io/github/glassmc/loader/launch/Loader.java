package io.github.glassmc.loader.launch;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;

public class Loader extends ClassLoader {

    private final ClassLoader parent = Loader.class.getClassLoader();

    private final List<ITransformer> transformers;

    public Loader(List<ITransformer> transformers) {
        this.transformers = transformers;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if(name.startsWith("java.") || name.startsWith("jdk.") || name.startsWith("sun.") || name.startsWith("com.sun.") || name.startsWith("javax.") || name.startsWith("org.xml.") || name.startsWith("org.w3c.")) {
            return this.parent.loadClass(name);
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

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return parent.getResources(name);
    }

    @Override
    public URL getResource(String name) {
        return parent.getResource(name);
    }

}
