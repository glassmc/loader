package com.github.glassmc.loader.api.loader;

public interface Transformer {
    boolean canTransform(String className);
    byte[] transform(String className, byte[] data);
}
