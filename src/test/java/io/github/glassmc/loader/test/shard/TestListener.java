package io.github.glassmc.loader.test.shard;

import io.github.glassmc.loader.Listener;

public class TestListener implements Listener {

    @Override
    public void run() {
        System.out.println("Test!");
    }

}
