package com.github.glassmc.loader.launch;

public interface ITransformer {
    byte[] transform(String className, byte[] data);
}
