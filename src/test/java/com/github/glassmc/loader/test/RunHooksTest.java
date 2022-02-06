package com.github.glassmc.loader.test;

import com.github.glassmc.loader.api.GlassLoader;
import com.github.glassmc.loader.impl.GlassLoaderImpl;
import org.junit.jupiter.api.Test;

public class RunHooksTest {

    @Test
    public void runHooksTest() {
        GlassLoader.getInstance().runHooks("test");
    }

}
