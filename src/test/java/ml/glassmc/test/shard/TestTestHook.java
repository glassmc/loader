package ml.glassmc.test.shard;

import ml.glassmc.loader.Hook;

public class TestTestHook implements Hook {

    @Override
    public void run() {
        System.out.println("Test hook!");
    }

}
