package com.github.glassmc.loader.api;

import com.github.glassmc.loader.impl.ShardSpecification;

import java.util.List;
import java.util.Map;

public interface ShardInfo {

    ShardSpecification getSpecification();

    Map<String, List<String>> getListeners();

    Environment getEnvironment();

    List<ShardInfo> getImplementations();

    interface Environment {
        List<ShardSpecification> getHas();
        List<ShardSpecification> getLacks();
    }

}
