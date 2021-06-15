package ml.glassmc.loader.launch;

public interface ITransformer {
    byte[] transform(String className, byte[] data);
}
