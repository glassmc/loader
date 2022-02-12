package com.github.glassmc.loader.impl;

import com.github.glassmc.loader.api.ShardInfo;

import java.util.List;
import java.util.Map;

public class ShardInfoImpl implements ShardInfo {

    private final ShardSpecification specification;
    private final Map<String, List<String>> listeners;
    private final Environment environment;
    private final List<ShardInfoImpl> implementations;

    private ShardInfoImpl parent;

    public ShardInfoImpl(ShardSpecification specification, Map<String, List<String>> listeners, Environment environment, List<ShardInfoImpl> implementations) {
        this.specification = specification;
        this.listeners = listeners;
        this.environment = environment;
        this.implementations = implementations;

        for(ShardInfoImpl implementation : this.implementations) {
            implementation.setParent(this);
        }
    }

    @Override
    public ShardSpecification getSpecification() {
        return this.specification;
    }

    @Override
    public Map<String, List<String>> getListeners() {
        return this.listeners;
    }

    @Override
    public Environment getEnvironment() {
        return this.environment;
    }

    @Override
    public List<ShardInfo> getImplementations() {
        return (List<ShardInfo>) (List) this.implementations;
    }

    public void setParent(ShardInfoImpl parent) {
        this.parent = parent;
    }

    public ShardInfoImpl getParent() {
        return parent;
    }

    public static class EnvironmentImpl implements Environment {

        private final List<ShardSpecification> has, lacks;

        public EnvironmentImpl(List<ShardSpecification> has, List<ShardSpecification> lacks) {
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
