package io.github.glassmc.loader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ShardInfo {

    private final ShardSpecification specification;
    private final Map<String, List<Class<? extends Listener>>> listeners;
    private final List<ShardSpecification> dependencies;
    private final List<ShardInfo> implementations;

    public ShardInfo(ShardSpecification specification, Map<String, List<Class<? extends Listener>>> listeners, List<ShardSpecification> dependencies, List<ShardInfo> implementations) {
        this.specification = specification;
        this.listeners = listeners;
        this.dependencies = dependencies;
        this.implementations = implementations;
    }

    public ShardSpecification getSpecification() {
        return this.specification;
    }

    public Map<String, List<Class<? extends Listener>>> getListeners() {
        Map<String, List<Class<? extends Listener>>> listeners = new LinkedHashMap<>();
        for(ShardInfo implementation : this.getImplementations()) {
            listeners.putAll(implementation.getListeners());
        }
        for(String hook : this.listeners.keySet()) {
            listeners.computeIfAbsent(hook, k -> new ArrayList<>()).addAll(this.listeners.get(hook));
        }
        return listeners;
    }

    public List<ShardSpecification> getDependencies() {
        return this.dependencies;
    }

    public List<ShardInfo> getImplementations() {
        return this.implementations;
    }

}
