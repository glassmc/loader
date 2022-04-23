package com.github.glassmc.loader.api;

import java.io.File;
import java.util.List;

/**
 * Used to modify and implement more advanced functionality than would be normally possible in the Glass ecosystem.
 */
public interface InternalLoader {

    /**
     * Invoked directly following the internal loader's initialization, which comes with a pre-scan of the shards in the classpath (prior to the actual loading of shards).
     */
    default void initialize() {

    }

    /**
     * Allows for the modification of the classpath, which brings in the possibility of loading shards from different directories (and more).
     * @param classpath The current classpath to be searched by the classloader.
     */
    default void modifyClassPath(List<File> classpath) {

    }

    /**
     * Makes it possible to select a specific version of a class given a list of {@link ClassDefinition}. One of its main use cases is to resolve library version conflicts.
     * @param className The name of the class being loaded.
     * @param possibleClasses A list of {@link ClassDefinition} that were discovered in the classpath.
     */
    default void filterClasses(String className, List<ClassDefinition> possibleClasses) {

    }

}
