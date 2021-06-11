package ml.glassmc.loader.client.test.shard

import ml.glassmc.loader.Listener

class TestClientInitializeListener : Listener {

    override fun run() {
        println("Test Hook!")
    }

}