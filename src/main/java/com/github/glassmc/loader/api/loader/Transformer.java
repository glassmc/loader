package com.github.glassmc.loader.api.loader;

public interface Transformer {

    default boolean acceptsBlank() {
        return false;
    }

    boolean canTransform(String className);
    byte[] transform(String className, byte[] data);
}
