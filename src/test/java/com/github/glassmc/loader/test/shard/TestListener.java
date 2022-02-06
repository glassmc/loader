package com.github.glassmc.loader.test.shard;

import com.github.glassmc.loader.api.Listener;

public class TestListener implements Listener {

    @Override
    public void run() {
        System.out.println("Test!");
    }

}
