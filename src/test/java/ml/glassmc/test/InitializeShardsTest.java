package ml.glassmc.test;

import ml.glassmc.loader.GlassLoader;
import org.junit.Test;

public class InitializeShardsTest {

    @Test
    public void test() {
        GlassLoader.getInstance().collectShardInfo();
    }

}
