package com.github.glassmc.loader.api.loader;

/**
 * Allows for the modification of loaded classes that are process by Glass.
 */
public interface Transformer {

    /**
     * @return Whether the transformer will process an empty class (likely defining its own class through some other method).
     */
    default boolean acceptsBlank() {
        return false;
    }

    /**
     * @param className The class in question.
     * @return Whether the transformer will modify the class in question. Best practice to only return true for classes that are being transformed, as opposed to handling this logic in the  <b>transform</b> method.
     */
    boolean canTransform(String className);

    /**
     * Modifies the loaded class' byte data.
     * @param className The name of the class being loaded.
     * @param data The byte array representing the class' raw data.
     * @return A modified byte array, with the modified class data.
     */
    byte[] transform(String className, byte[] data);
}
