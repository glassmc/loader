package ml.glassmc.loader.test;

import ml.glassmc.loader.GlassLoader;
import ml.glassmc.loader.test.shard.TestHook;
import org.junit.jupiter.api.Test;

public class RunHooksTest {

    @Test
    public void runHooksTest() {
        GlassLoader.getInstance().runHooks(TestHook.class);
    }

}
