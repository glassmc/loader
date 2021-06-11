package ml.glassmc.loader.client.test.shard

class TestAPI {

    private var counter = 0

    fun add() {
        counter++
    }

    fun get(): Int {
        return counter
    }
}