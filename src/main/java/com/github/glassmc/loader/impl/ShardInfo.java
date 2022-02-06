package com.github.glassmc.loader.impl;

import java.util.List;
import java.util.Map;

public class ShardInfo {

    private final ShardSpecification specification;
    private final Map<String, List<String>> listeners;
    private final Environment environment;
    private final List<ShardInfo> implementations;

    private ShardInfo parent;

    public ShardInfo(ShardSpecification specification, Map<String, List<String>> listeners, Environment environment, List<ShardInfo> implementations) {
        this.specification = specification;
        this.listeners = listeners;
        this.environment = environment;
        this.implementations = implementations;

        for(ShardInfo implementation : this.implementations) {
            implementation.setParent(this);
        }
    }

    public ShardSpecification getSpecification() {
        return this.specification;
    }

    public Map<String, List<String>> getListeners() {
        return this.listeners;
    }

    public Environment getEnvironment() {
        return this.environment;
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

    public static class Environment {

        private final List<ShardSpecification> has, lacks;

        public Environment(List<ShardSpecification> has, List<ShardSpecification> lacks) {
            this.has = has;
            this.lacks = lacks;
        }

        public List<ShardSpecification> getHas() {
            return has;
        }

        public List<ShardSpecification> getLacks() {
            return lacks;
        }

    }

}
