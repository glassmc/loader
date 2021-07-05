package io.github.glassmc.loader.loader;

public interface ITransformer {
    byte[] transform(String className, byte[] data);
}
