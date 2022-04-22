package com.github.glassmc.loader.impl;

import com.github.glassmc.loader.api.InternalLoader;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class InternalLoaderImpl implements InternalLoader {

    private final File shardsFile = new File("shards");

    public InternalLoaderImpl() {
        if (!shardsFile.exists()) {
            shardsFile.mkdirs();
        }
    }

    @Override
    public void addClassPath(List<File> classpath) {
        if(shardsFile.exists()) {
            classpath.addAll(Arrays.asList(Objects.requireNonNull(shardsFile.listFiles())));
        }
    }

}
