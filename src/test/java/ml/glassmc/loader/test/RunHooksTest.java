package ml.glassmc.loader.test;

import ml.glassmc.loader.GlassLoader;
import org.junit.jupiter.api.Test;

public class RunHooksTest {

    @Test
    public void runHooksTest() {
        GlassLoader.getInstance().runHooks("test");
    }

}
