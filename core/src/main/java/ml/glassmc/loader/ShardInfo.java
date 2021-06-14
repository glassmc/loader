package ml.glassmc.loader;

import java.util.List;
import java.util.Map;

public class ShardInfo {

    private final ShardSpecification specification;
    private final Map<Class<?>, Class<? extends Listener>> listeners;
    private final List<ShardSpecification> dependencies;
    private final List<ShardInfo> implementations;

    public ShardInfo(ShardSpecification specification, Map<Class<?>, Class<? extends Listener>> listeners, List<ShardSpecification> dependencies, List<ShardInfo> implementations) {
        this.specification = specification;
        this.listeners = listeners;
        this.dependencies = dependencies;
        this.implementations = implementations;
    }

    public ShardSpecification getSpecification() {
        return this.specification;
    }

    public Map<Class<?>, Class<? extends Listener>> getListeners() {
        Map<Class<?>, Class<? extends Listener>> listeners = this.listeners;
        for(ShardInfo implementation : this.getImplementations()) {
            listeners.putAll(implementation.getListeners());
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
