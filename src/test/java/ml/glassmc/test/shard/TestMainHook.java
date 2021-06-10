package ml.glassmc.test.shard;

import ml.glassmc.loader.GlassLoader;
import ml.glassmc.loader.Hook;

public class TestMainHook implements Hook {

    @Override
    public void run() {
        GlassLoader.getInstance().registerAPI(new TestAPI());
    }

}
