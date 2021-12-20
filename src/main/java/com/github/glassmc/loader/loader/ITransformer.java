package com.github.glassmc.loader.loader;

public interface ITransformer {
    boolean canTransform(String className);
    byte[] transform(String className, byte[] data);
}
