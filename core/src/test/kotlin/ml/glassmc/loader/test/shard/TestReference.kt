package ml.glassmc.loader.test.shard

import ml.glassmc.loader.Listener
import ml.glassmc.loader.Reference
import java.util.HashMap

class TestReference : Reference {

    override val listeners: Map<Class<*>, Class<out Listener>>
        get() = object: HashMap<Class<*>, Class<out Listener>>() {
            init {
                put(TestHook::class.java, TestTestListener::class.java)
            }
        }

}