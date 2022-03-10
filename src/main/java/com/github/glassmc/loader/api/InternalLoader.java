package com.github.glassmc.loader.api;

import java.io.File;
import java.util.List;

public interface InternalLoader {

    void initialize();
    void addClassPath(List<File> classpath);

}
