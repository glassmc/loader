package ml.glassmc.loader.test.shard

class TestAPI {

    private var counter = 0

    fun add() {
        counter++
    }

    fun get(): Int {
        return counter
    }
}