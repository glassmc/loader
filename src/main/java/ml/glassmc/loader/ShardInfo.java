package ml.glassmc.loader;

import java.util.List;
import java.util.Map;

public class ShardInfo {

    private final ShardSpecification specification;
    private final Map<Class<?>, Class<? extends Hook>> hooks;
    private final List<ShardSpecification> dependencies;

    public ShardInfo(ShardSpecification specification, Map<Class<?>, Class<? extends Hook>> hooks, List<ShardSpecification> dependencies) {
        this.specification = specification;
        this.hooks = hooks;
        this.dependencies = dependencies;
    }

    public ShardSpecification getSpecification() {
        return specification;
    }

    public Map<Class<?>, Class<? extends Hook>> getHooks() {
        return hooks;
    }

    public List<ShardSpecification> getDependencies() {
        return dependencies;
    }

}
