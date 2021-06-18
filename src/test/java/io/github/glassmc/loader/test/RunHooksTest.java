package io.github.glassmc.loader.test;

import io.github.glassmc.loader.GlassLoader;
import org.junit.jupiter.api.Test;

public class RunHooksTest {

    @Test
    public void runHooksTest() {
        GlassLoader.getInstance().runHooks("test");
    }

}
