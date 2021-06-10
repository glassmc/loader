package ml.glassmc.test;

import ml.glassmc.loader.GlassLoader;
import ml.glassmc.loader.hook.ClientHookType;
import org.junit.Test;

public class InitializeShardsTest {

    @Test
    public void test() {
        GlassLoader.getInstance().runHooks(ClientHookType.class);
    }

}
