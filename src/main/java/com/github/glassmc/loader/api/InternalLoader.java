package com.github.glassmc.loader.api;

import java.io.File;
import java.util.List;

public interface InternalLoader {

    default void initialize() {

    }

    default void addClassPath(List<File> classpath) {

    }

    default void filterClasses(String className, List<ClassDefinition> possibleClasses) {

    }

}
