package ml.glassmc.loader.test.shard

import ml.glassmc.loader.Listener

class TestTestListener : Listener {

    override fun run() {
        println("Test hook!")
    }

}