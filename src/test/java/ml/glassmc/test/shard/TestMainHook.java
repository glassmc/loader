package ml.glassmc.test.shard;

import ml.glassmc.loader.GlassLoader;
import ml.glassmc.loader.Hook;

public class TestMainHook implements Hook {

    @Override
    public void run() {
        System.out.println("Hook!");
        GlassLoader.getInstance().registerAPI(new TestAPI());
    }

}
