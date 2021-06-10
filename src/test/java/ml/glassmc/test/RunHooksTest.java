package ml.glassmc.test;

import ml.glassmc.loader.GlassLoader;
import ml.glassmc.test.shard.TestHookType;
import org.junit.Test;

public class RunHooksTest {

    @Test
    public void test() {
        GlassLoader.getInstance().runHooks(TestHookType.class);
    }

}
