package ml.glassmc.loader.test.shard;

import ml.glassmc.loader.Listener;
import ml.glassmc.loader.Reference;

import java.util.HashMap;
import java.util.Map;

public class TestReference implements Reference {

    @Override
    public Map<Class<?>, Class<? extends Listener>> getListeners() {
        return new HashMap<Class<?>, Class<? extends Listener>>() {
            {
                put(TestHook.class, TestListener.class);
            }
        };
    }

}
