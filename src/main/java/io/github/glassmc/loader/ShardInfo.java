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

    private ShardInfo parent;

    public ShardInfo(ShardSpecification specification, Map<String, List<Class<? extends Listener>>> listeners, List<ShardSpecification> dependencies, List<ShardInfo> implementations) {
        this.specification = specification;
        this.listeners = listeners;
        this.dependencies = dependencies;
        this.implementations = implementations;

        for(ShardInfo implementation : this.implementations) {
            implementation.setParent(this);
        }
    }

    public ShardSpecification getSpecification() {
        return this.specification;
    }

    public Map<String, List<Class<? extends Listener>>> getListeners() {
        return this.listeners;
    }

    public List<ShardSpecification> getDependencies() {
        return this.dependencies;
    }

    public List<ShardInfo> getImplementations() {
        return this.implementations;
    }

    public void setParent(ShardInfo parent) {
        this.parent = parent;
    }

    public ShardInfo getParent() {
        return parent;
    }
}
