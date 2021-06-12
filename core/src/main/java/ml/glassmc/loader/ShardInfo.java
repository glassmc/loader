package ml.glassmc.loader;

import java.util.List;
import java.util.Map;

public class ShardInfo {

    private final ShardSpecification specification;
    private final Map<Class<?>, Class<? extends Listener>> listeners;
    private final List<ShardSpecification> dependencies;

    public ShardInfo(ShardSpecification specification, Map<Class<?>, Class<? extends Listener>> listeners, List<ShardSpecification> dependencies) {
        this.specification = specification;
        this.listeners = listeners;
        this.dependencies = dependencies;
    }

    public ShardSpecification getSpecification() {
        return specification;
    }

    public Map<Class<?>, Class<? extends Listener>> getListeners() {
        return listeners;
    }

    public List<ShardSpecification> getDependencies() {
        return dependencies;
    }

}
