package ml.glassmc.test.shard;

import ml.glassmc.loader.Hook;
import ml.glassmc.loader.Reference;

import java.util.HashMap;
import java.util.Map;

public class TestReference implements Reference {

    @Override
    public Map<Class<?>, Class<? extends Hook>> getHooks() {
        return new HashMap<Class<?>, Class<? extends Hook>>() {
            {
                put(TestHookType.class, TestTestHook.class);
            }
        };
    }

}
